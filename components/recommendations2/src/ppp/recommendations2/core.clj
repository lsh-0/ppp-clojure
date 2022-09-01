(ns ppp.recommendations2.core
  (:require
   [ppp.utils.interface :as utils :refer [spy]]
   [ppp.api-raml.interface :as api-raml]
   [ppp.lax.interface :as article-api]
   [ppp.journal-cms.interface :as journal-cms]
   [ppp.search.interface :as search]
   [orchestra.core :refer [defn-spec]]
   [clojure.spec.alpha :as s]
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

(defn article-comparator
  "compares two articles, sorting by article type if types are different, or published date if types are the same."
  [a b]
  (let [at (:type a)
        bt (:type b)]
    (if (= at bt)
      (compare (:published a) (:published b))
      (compare (get article-order at) (get article-order bt)))))

(defn find-article
  [id api-key]
  (let [results (article-api/article-version-list id {:api-key api-key})]
    (utils/spit-json results "find-article.json")
    results))

(defn find-related-articles
  [id api-key]
  ;; https://github.com/elifesciences/recommendations/blob/5a9d9c929b7d81430a52fe84fd4a1220efb79509/src/bootstrap.php#L138-L167
  (let [;; a mixed list of article types
        results (article-api/related-article-list id {:api-key api-key})
        [content error?] (api-raml/handle-api-response results)]
    (if error?
      nil
      (do (utils/spit-json content "find-related-articles.json")
          (sort article-comparator content)))))

(defn find-collections
  ;; https://github.com/elifesciences/recommendations/blob/5a9d9c929b7d81430a52fe84fd4a1220efb79509/src/bootstrap.php#L169-L171
  [id api-key]
  (let [results (journal-cms/collections-list {:containing-list [(str "article/" id)] :per-page 100 :api-key api-key})
        [content error?] (api-raml/handle-api-response results)]
    (if error?
      nil
      (do (utils/spit-json content "find-collections.json")
          (:items content)))))

(defn find-articles-by-subject
  [id article-versions]
  ;; https://github.com/elifesciences/recommendations/blob/5a9d9c929b7d81430a52fe84fd4a1220efb79509/src/bootstrap.php#L187-L192
  (let [search-for nil
        search-for-type ["editorial" "feature" "insight" "research-advance" "research-article" "research-communication" "registered-report" "replication-study" "review-article" "scientific-correspondence" "short-report" "tools-resources"]
        sort-by "date"
        subject-id (get-in article-versions [:versions 0 :subjects 0 :id])
        search-for-subject [subject-id]] ;; 'subjected' trait
    (when subject-id
      (let [search-results (search/search search-for sort-by search-for-type {:subject-list search-for-subject})
            [content error?] (api-raml/handle-api-response search-results)]
        (if error?
          nil
          (let [self? (fn [article]
                        (= (str id) (:id article)))

                ;; `->slice(0, 5)` => (take 4 ...)
                content (remove self? (take 4 (:items content))) ;; what recommendations is doing
                ;;content (take 4 (remove self? content)) ;; this seems better
                ]

            (do (utils/spit-json content "find-articles-by-subject.json")
                content
                )))))))

(defn find-podcast-episodes
  [id]
  ;; https://github.com/elifesciences/recommendations/blob/develop/src/bootstrap.php#L195-L209
  (let [podcast-episode-list (journal-cms/podcast-episode-list {:containing-list [(str "article/" id)] :per-page 100
                                                                :request {:debug? false}})
        [content error?] (api-raml/handle-api-response podcast-episode-list)]
    (if error?
      nil ;; log error? assume it's been handled elsewhere and carry on? push into `handle-api-response` ?


      ;; todo: ;; https://github.com/elifesciences/recommendations/blob/5a9d9c929b7d81430a52fe84fd4a1220efb79509/src/bootstrap.php#L198-L206


      (do (utils/spit-json content "find-podcast-episodes.json")
          (:items content)))))

(defn-spec find-article-recommendations :ppp/list-of-maps
  [id :api-raml/manuscript-id, api-key :api-raml/api-key]
  (let [article-versions (find-article id api-key)
        [content error?] (api-raml/handle-api-response article-versions)]
    (if error?
      [content]

      (let [;; article with `id` exists, now find the rest
            relations #(find-related-articles id api-key)
            collections #(find-collections id api-key)
            recent-articles-with-subject #(find-articles-by-subject id content)
            podcasts #(find-podcast-episodes id)
            runner (fn [f]
                     (try
                       (f)
                       (catch Exception e
                         (println "error calling" f)
                         (println e)
                         )))

            ;; do the requests synchronously for now. switch `mapv` to `pmap` to do them asynchronously.
            [relations collections recent-articles-with-subject podcasts]
            (mapv runner [relations collections recent-articles-with-subject podcasts])

            recommendations (reduce into [relations collections podcasts])
            num-recommendations (count recommendations)
            recommendations (if (>= num-recommendations 3)
                              recommendations

                              ;; scrounge about for more recommendations in the search results,
                              ;; ignoring anything already recommended,
                              ;; taking no more than needed to make up the difference.
                              ;; see:
                              ;; - https://github.com/elifesciences/recommendations/blob/5a9d9c929b7d81430a52fe84fd4a1220efb79509/src/bootstrap.php#L225-L229
                              ;; - https://github.com/elifesciences/recommendations/blob/cdd445d7abe44d85acbdf7d6404cc52b514db97f/src/functions.php#L10-L44
                              (let [idx (set recommendations)
                                    num (- 3 num-recommendations)]
                                (into recommendations
                                      (take num
                                            (take-while #(not (contains? idx %)) recent-articles-with-subject)))))
            ]
        recommendations))))

(defn find-recommendations
  [type id api-key]
  (case type
    "article" (find-article-recommendations id api-key)
    nil))

(defn paginate
  [content page per-page order]
  (let [offset (* per-page (dec page))
        items (vec (take per-page (nthrest content offset)))]
    {:total (count items)
     :items items}))

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

        type-content (first results) ;; the only supported type is 'article'
        error? (and (contains? type-content :http-resp)
                    (api-raml/http-error? type-content))
        ]
    (if error?
      type-content

      (let [;; todo: this pagination will have to happen in `find-recommendations` as there is some weird abstract-fetching logic that happens after pagination has occurred.
            content (paginate results (:page kwargs) (:per-page kwargs) (:order kwargs))
            content-type-pair (negotiate content (:content-type-list kwargs))
            [content-type, content-type-opts] content-type-pair]

        {:content content
         :content-type content-type
         :content-type-version (:version content-type-opts)
         :content-type-version-deprecated? (not= content-type-pair api-raml/recommendations-list-v2)
         :authenticated? false}))))
