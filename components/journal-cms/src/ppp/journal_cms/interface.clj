(ns ppp.journal-cms.interface
  (:require
   [ppp.journal-cms.core :as journal-cms]
   ))

;; subject-list
;; - https://github.com/elifesciences/api-raml/blob/develop/dist/api.raml#L162-L169
;; containing-list
;; - https://github.com/elifesciences/api-raml/blob/develop/dist/api.raml#L195-L202
(defn collections-list
  ([kwargs]
   (journal-cms/collections-list kwargs))
  ([subject-list containing-list content-type-list page per-page order api-key kwargs]
   (journal-cms/collections-list {:subject-list subject-list
                                  :containing-list containing-list
                                  :content-type-list content-type-list
                                  :page page
                                  :per-page per-page
                                  :order order
                                  :api-key api-key})))

(defn collection
  ([id kwargs]
   (journal-cms/collection id kwargs))
  ([id content-type-list api-key kwargs]
   (journal-cms/collection id (merge kwargs {:content-type-list content-type-list
                                             :api-key api-key}))))
                               
   
