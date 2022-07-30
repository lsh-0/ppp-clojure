(ns ppp.api-raml.specs
  (:require
   [clojure.spec.alpha :as s]))

(s/def :api-raml/article-list (s/coll-of map?))

(s/def ::uri uri?)
(s/def :error/type (s/or ::uri #{"about:blank"}))
(s/def :error/title string?)
(s/def :error/detail string?)
(s/def :api-raml/error (s/keys :req-un [:error/type :error/title :error/detail]))
(s/def :api-raml/order #{"asc" "desc"})
(s/def :api-raml/manuscript-id pos-int?)


;; components fulfilling API endpoints are given an ordered list of supported content-type+version pairs:
;;   [("application/vnd.elife.article-poa+json" {:version 2}), ("application/vnd.elife.article-poa+json" {:version 1})]

(s/def :api-raml/content-type string?)
(s/def :api-raml/content-type-version pos-int?)
(s/def :api-raml/content-type-params (s/keys :req-un [::content-type-version]))
(s/def :api-raml/content-type-pair (s/tuple :api-raml/content-type :api-raml/content-type-params))
(s/def :api-raml/content-type-list (s/coll-of :api-raml/content-type-pair))
