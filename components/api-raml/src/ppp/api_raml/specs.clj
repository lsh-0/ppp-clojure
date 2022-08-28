(ns ppp.api-raml.specs
  (:require
   [clojure.spec.alpha :as s]))

;; keep the specs the *simple* and *shallow*, vs complex and deeply nested.

;; this seems like a natural place to replicate the api-raml json-schema specs but I would recommend against it.
;; they are large, complex and time consuming to validate. I doubt they would help during development and not at all in prod.

(s/def ::uri uri?)
(s/def :error/type (s/or ::uri #{"about:blank"}))
(s/def :error/title string?)
(s/def :error/detail string?)
(s/def :api-raml/error (s/keys :req-un [:error/type :error/title :error/detail]))
(s/def :api-raml/order #{"asc" "desc"})
(s/def :api-raml/manuscript-id pos-int?)
(s/def :api-raml/id (s/or :string string?, :integer pos-int?))
(s/def :api-raml/api-key (s/nilable string?))

(s/def :ppp/list-of-maps (s/coll-of map?))
(s/def :ppp/pair-of-map+bool (s/tuple map? boolean?))

;; components fulfilling API endpoints are given an ordered list of supported content-type+version pairs:
;;   [("application/vnd.elife.article-poa+json" {:version 2}), ("application/vnd.elife.article-poa+json" {:version 1})]

(s/def :api-raml/authenticated? boolean?)
(s/def :api-raml/content coll?) ;; keep these specs separate from api-raml specs
(s/def :api-raml/content-type string?)
(s/def :api-raml/content-type-version (s/nilable pos-int?)) ;; 'application/json' has no version for example
(s/def :api-raml/content-type-version-deprecated boolean?)
(s/def :api-raml/content-type-params (s/keys :req-un [::content-type-version]))
(s/def :api-raml/content-type-pair (s/tuple :api-raml/content-type :api-raml/content-type-params))
(s/def :api-raml/content-type-list (s/coll-of :api-raml/content-type-pair))

(s/def :clj-http/status pos-int?)
(s/def :clj-http/http-resp (s/keys :req-un [:clj-http/status]))

;; what each component serving as a microservice should return
(s/def :ppp.component/response (s/keys :req-un [:api-raml/content
                                                :api-raml/content-type
                                                :api-raml/content-type-version
                                                :api-raml/content-type-version-deprecated?
                                                :api-raml/authenticated?]
                                       ;; only present if a http request was made
                                       :opt-un [:clj-http/http-resp]))
