(ns ppp.recommendations2.core-test
  (:require
   [clojure.test :as test :refer [deftest is testing use-fixtures]]
   [ppp.recommendations2.core :as core]
   [ppp.api-raml.interface :as api-raml]
   [clj-http.fake :refer [with-global-fake-routes-in-isolation]]
   ))

;; https://github.com/elifesciences/recommendations/blob/develop/test/RecommendationsTest.php

(defn no-http-calls-fixture
  [test-fn]
  (let [fake-routes {}]
    (with-global-fake-routes-in-isolation fake-routes
      (test-fn))))

(defn spy
  [x]
  (println "spy:" x)
  x)

(use-fixtures :each no-http-calls-fixture)

;; ---

(deftest recommendations-list--negotiates-type
  (testing "supported content types are supported :)"
    (let [cases
          [[api-raml/recommendations-list-v2, api-raml/recommendations-list-v2] ;; specific latest
           [api-raml/recommendations-list-v1, api-raml/recommendations-list-v1] ;; specific older
           [api-raml/recommendations-list, api-raml/recommendations-list-v2] ;; no version
           [api-raml/application-json, api-raml/recommendations-list-v2] ;; generic content
           ]
          dummy-article {:content {:status 200}}
          empty-response {}]
      (doseq [[given-content-type, expected-content-type] cases]
        (with-redefs [core/find-article (constantly dummy-article)
                      core/find-related-articles (constantly empty-response)
                      core/find-collections (constantly empty-response)
                      core/find-articles-by-subject (constantly empty-response)
                      core/find-podcast-episodes (constantly empty-response)
                      ]
          (let [result (core/recommendation-list "articles" "1234" {:content-type-list [given-content-type]})]
            (is (= expected-content-type (:content-type-pair result)))))))))

(deftest recommendations-list--article-not-found
  (testing "failure to find an article skips any further lookups and passed the 'Not Found' error through."
    (let [expected (api-raml/http-error-response {:status 404})]
      (with-redefs [core/find-article (constantly expected)]
        (is (= expected (core/recommendation-list "articles" "1234")))))))

(deftest recommendations-list--article-found-no-recommendations
  (testing "it returns an empty recommendations for an article that exists"
    nil))
