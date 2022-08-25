(ns ppp.recommendations2.core
  (:require
   [ppp.api-raml.interface :as api-raml]
   [ppp.lax.interface :as article-api]
   [ppp.journal-cms.interface :as journal-cms]
   [ppp.search.interface :as search]
   ))

(def article-order
  {:retraction 1,
   :correction 2,
   :external-article 3,
   :registered-report 4,
   :replication-study 5,
   :research-advance 6,
   :scientific-correspondence 7,
   :research-article 8,
   :research-communication 8,
   :tools-resources 9,
   :feature 10,
   :insight 11,
   :editorial 12,
   :short-report 13,
   :review-article 14})

(defn spy
  [x]
  (println "spy:" x)
  x)

(defn find-article
  [id api-key]
  (article-api/article-version-list id {:api-key api-key}))

(defn find-related-articles
  [id api-key]
  ;; https://github.com/elifesciences/recommendations/blob/5a9d9c929b7d81430a52fe84fd4a1220efb79509/src/bootstrap.php#L138-L167
  (let [;; a mixed list of article types
        article-list (:content (article-api/related-article-list id {:api-key api-key}))
        comparator (fn [a b]
                     (let [at (:type a)
                           bt (:type b)]
                       (if (= at bt)
                         (compare (:published a) (:published b))
                         (compare (get article-order at) (get article-order bt)))))]
    (sort comparator article-list)))

(defn find-collections
  ;; https://github.com/elifesciences/recommendations/blob/5a9d9c929b7d81430a52fe84fd4a1220efb79509/src/bootstrap.php#L169-L171
  [id api-key]
  (let [collection (journal-cms/collection id {:api-key api-key})
        [content error?] (api-raml/handle-api-response collection)]
    (if error?
      nil
      content)))

(defn find-articles-by-subject
  [article-version-list]
  ;; https://github.com/elifesciences/recommendations/blob/5a9d9c929b7d81430a52fe84fd4a1220efb79509/src/bootstrap.php#L187-L192
  (let [subject-id (get-in article-version-list [0 :subjects 0 :id])
        search-for subject-id
        search-sort "date"
        search-type-list ["editorial" "feature" "insight" "research-advance" "research-article" "research-communication" "registered-report" "replication-study" "review-article" "scientific-correspondence" "short-report" "tools-resources"]
        ]
    (when subject-id
      (let [search-results (search/search search-for search-sort search-type-list {})
            [content error?] (api-raml/handle-api-response search-results)]
        (if error?
          nil ;; log error? assume it's been handled elsewhere and carry on? push into `handle-api-response` ?
          content)))))

(defn find-podcast-episodes
  [id]
  ;; https://github.com/elifesciences/recommendations/blob/develop/src/bootstrap.php#L195-L209
  (let [podcast-episode-list (journal-cms/podcast-episode-list {:containing-list [id]
                                                                :per-page 100})
        [content error?] (api-raml/handle-api-response podcast-episode-list)]
    (if error?
      nil ;; log error? assume it's been handled elsewhere and carry on? push into `handle-api-response` ?
      ;; todo:
      ;; https://github.com/elifesciences/recommendations/blob/5a9d9c929b7d81430a52fe84fd4a1220efb79509/src/bootstrap.php#L198-L206
      content)))

(defn find-article-recommendations
  [id api-key]
  (let [;; todo: this is an expensive existence check. a HEAD request would be cheaper but that would assume implementation knowledge.
        ;; a volatile msid cache could be possible
        article-version-list (find-article id api-key)

        [content error?] (api-raml/handle-api-response article-version-list)]
    (if error?
      content

      (let [;; article with `id` exists, now find the rest
            relations #(find-related-articles id api-key)
            collections #(find-collections id api-key)
            recent-articles-with-subject #(find-articles-by-subject article-version-list)
            podcasts #(find-podcast-episodes id)

            ;; call above functions in parallel (pmap ...), return a single list of results (reduce into ...)
            results (reduce into (pmap #(%) [relations collections recent-articles-with-subject podcasts]))

            ;; what is this doing? some kind of de-duplication?
            ;; https://github.com/elifesciences/recommendations/blob/5a9d9c929b7d81430a52fe84fd4a1220efb79509/src/bootstrap.php#L227-L229
            ;; https://github.com/elifesciences/recommendations/blob/cdd445d7abe44d85acbdf7d6404cc52b514db97f/src/functions.php#L10-L30
            ]
        results))))

(defn find-recommendations
  [type id api-key]
  (case type
    "articles" (find-article-recommendations id api-key)
    nil))

(defn paginate
  [content page per-page order]
  (let [offset (* per-page (dec page))]
    (take per-page (nthrest content offset))))

(defn negotiate
  [content acceptable-content-type-list]
  ;; what magic is this doing?
  ;; https://github.com/elifesciences/recommendations/blob/cf601c2290834d8cad34716e04a5da1982f9d820/src/bootstrap.php#L292-L295
  (let [v2-compatible? true ;; assume whatever content is returned is v1 and v2 compatible
        latest-content-type (first api-raml/recommendations-list-content-type)
        ;; todo: recurse through all acceptable content types?
        accepts-content-type (first acceptable-content-type-list) 
        [content-type content-type-opts] accepts-content-type
        version (:version content-type-opts)]
    (cond
      ;; request specified the generic any '*' or '*/*' or 'application/*' or 'application/json'
      ;; return the most specific we can
      (= content-type api-raml/application-json) latest-content-type

      ;; request specified a non-generic content-type but no version
      ;; return the most specific we can
      (not version) latest-content-type

      ;; request specified a non-generic, versioned content-type
      v2-compatible? (first acceptable-content-type-list)

      ;; could not negotiate a content-type
      :else (api-raml/http-error-response {:status 406}))))

(defn recommendation-list
  [type id & [kwargs]]
  (let [defaults {:content-type-list api-raml/recommendations-list-content-type
                  :page 1
                  :per-page 20
                  :order "desc"
                  :api-key nil}
        kwargs (merge defaults kwargs)
        results (find-recommendations type id (:api-key kwargs))

        ;; non-200 responses may be passed through. 
        [content error?] (if (api-raml/api-response? results)
                           (api-raml/handle-api-response results)
                           [results false])]
    (if error?
      content

      (let [content (paginate content (:page kwargs) (:per-page kwargs) (:order kwargs))
            content-type-pair (negotiate content (:content-type-list kwargs))
            [content-type, content-type-opts] content-type-pair]

        {:content content
         :content-type content-type
         :content-type-pair content-type-pair ;; todo: this is new and saves a bunch of destructuring.
         :content-type-version (:version content-type-opts)
         :content-type-version-deprecated? (not= content-type-pair api-raml/recommendations-list-v2)
         :authenticated? false}))))
