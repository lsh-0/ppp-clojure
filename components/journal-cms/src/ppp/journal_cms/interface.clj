(ns ppp.journal-cms.interface
  (:require
   [ppp.journal-cms.core :as journal-cms]
   ))

(defn collections-list
  ([kwargs]
   (journal-cms/collections-list kwargs))
  ([subject-list containing-list content-type-list page per-page order api-key kwargs]
   (journal-cms/collections-list (merge kwargs {:subject-list subject-list
                                                :containing-list containing-list
                                                :content-type-list content-type-list
                                                :page page
                                                :per-page per-page
                                                :order order
                                                :api-key api-key}))))

(defn collection
  ([id kwargs]
   (journal-cms/collection id kwargs))
  ([id content-type-list api-key kwargs]
   (journal-cms/collection id (merge kwargs {:content-type-list content-type-list
                                             :api-key api-key}))))

(defn podcast-episode-list
  ([kwargs]
   (journal-cms/podcast-episode-list kwargs))
  ([containing-list content-type-list page per-page order api-key kwargs]
   (journal-cms/podcast-episode-list (merge kwargs {:containing-list containing-list
                                                    :content-type-list content-type-list
                                                    :page page
                                                    :per-page per-page
                                                    :order order
                                                    :api-key api-key}))))
