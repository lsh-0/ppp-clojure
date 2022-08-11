(ns ppp.recommendations2.core
  (:require
   [ppp.api-raml.interface :as api-raml]
   [ppp.lax.interface :as article-api]
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
   :tools-resourcesk 9,
   :feature 10,
   :insight 11,
   :editorial 12,
   :short-report 13,
   :review-article 14})

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
  [id]
  nil)

(defn find-articles-by-subject
  [article-version-list]
  ;; https://github.com/elifesciences/recommendations/blob/5a9d9c929b7d81430a52fe84fd4a1220efb79509/src/bootstrap.php#L187-L192
  (when-let [subject-id (get-in article-version-list [0 :subjects 0 :id])]
    nil))

(defn find-podcast-episodes
  [id]
  ;; https://github.com/elifesciences/recommendations/blob/develop/src/bootstrap.php#L195-L209
  nil)

(defn find-article-recommendations
  [id api-key]
  (if-let [article-version-list (find-article id api-key)]
    ;; article exists, now find the rest
    (let [relations #(find-related-articles id api-key)
          collections #(find-collections id)
          recent-articles-with-subject #(find-articles-by-subject article-version-list)
          podcasts #(find-podcast-episodes id)

          results (reduce into (pmap #(%) [relations collections recent-articles-with-subject podcasts]))

          ;; what is this doing? some kind of de-duplication?
          ;; https://github.com/elifesciences/recommendations/blob/5a9d9c929b7d81430a52fe84fd4a1220efb79509/src/bootstrap.php#L227-L229
          ;; https://github.com/elifesciences/recommendations/blob/cdd445d7abe44d85acbdf7d6404cc52b514db97f/src/functions.php#L10-L30
          
          ]
      results)))

(defn find-recommendations
  [type id]
  (case type
    "articles" (find-article-recommendations id)
    nil))

(defn paginate
  [content page per-page order]
  (let [offset (* per-page (dec page))]
    (take per-page (nthrest content offset))))

(defn negotiate
  [content acceptable-content-type-list]
  ;; what magic is this doing?
  ;; https://github.com/elifesciences/recommendations/blob/cf601c2290834d8cad34716e04a5da1982f9d820/src/bootstrap.php#L292-L295
  (first acceptable-content-type-list))

(defn recommendation-list
  [type id & [kwargs]]
  (let [defaults {:content-type-list [api-raml/recommendations-list-v2
                                      api-raml/recommendations-list-v1]
                  :page 1
                  :per-page 20
                  :order "desc"
                  :api-key nil}
        kwargs (merge defaults kwargs)
        content (find-recommendations type id)
        content (paginate content (:page kwargs) (:per-page kwargs) (:order kwargs))
        content-type-pair (negotiate content (:content-type-list kwargs))
        [content-type, content-type-version] content-type-pair]
    {:content content
     :content-type content-type
     :content-type-version content-type-version
     :content-type-version-deprecated? (not= content-type-pair api-raml/recommendations-list-v2)
     :authenticated? false}))
