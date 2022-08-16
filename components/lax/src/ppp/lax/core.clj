(ns ppp.lax.core
  (:require
   [ppp.api-raml.interface :as api-raml :refer [api-request]]
   ))

(defn article-list
  [& [kwargs]]
  (let [defaults {:content-type-list [api-raml/article-list-v1]
                  :page 1
                  :per-page 20
                  :order "desc"
                  :api-key nil}]
    (api-request "/articles" (merge defaults kwargs))))

(defn article
  [id & [kwargs]]
  (let [defaults {:content-type-list [api-raml/vor-v6 api-raml/poa-v3
                                      api-raml/vor-v5 api-raml/poa-v2]}]
    (api-request (str "/articles/" id) (merge defaults kwargs))))

(defn related-article-list
  [id & [kwargs]]
  (let [defaults {:content-type-list [api-raml/related-article-list-v1]}]
    (api-request (str "/articles/" id "/related") (merge defaults kwargs))))

(defn article-version-list
  [id & [kwargs]]
  (let [defaults {:content-type-list [api-raml/article-history-v1]}]
    (api-request (str "/articles/" id "/versions") (merge defaults kwargs))))

(defn article-version
  [id version & [kwargs]]
  (let [defaults {:content-type-list [api-raml/vor-v6 api-raml/poa-v3
                                      api-raml/vor-v5 api-raml/poa-v2]}]
    (api-request (str "/articles/" id "/versions/" version) (merge defaults kwargs))))
