(ns ppp.journal-cms.core
  (:require
   [ppp.api-raml.interface :as api-raml]))

(defn collections-list
  [& [kwargs]]
  (let [defaults {:subject-list []
                  :containing-list []
                  :content-type-list [api-raml/collections-list-v1]
                  :page 1
                  :per-page 20
                  :order "desc"
                  :api-key nil}]
    (api-raml/api-request "/collections" (merge defaults kwargs))))

(defn collection
  [id & [kwargs]]
  (let [defaults {:content-type-list [api-raml/collection-v2
                                      api-raml/collection-v1]
                  :api-key nil}]
    (api-raml/api-request (str "/collections/" id) (merge defaults kwargs))))
