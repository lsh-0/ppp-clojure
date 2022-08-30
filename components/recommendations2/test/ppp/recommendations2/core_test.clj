(ns ppp.recommendations2.core-test
  (:require
   [clojure.test :as test :refer [deftest is testing use-fixtures]]
   [ppp.recommendations2.core :as core]
   [ppp.api-raml.interface :as api-raml]
   [ppp.utils.interface :as utils]
   [clj-http.fake :refer [with-global-fake-routes-in-isolation]]
   [me.raynes.fs :as fs]
   [clojure.java.io :as io]
   [cheshire.core :as json]
   ))

;; https://github.com/elifesciences/recommendations/blob/develop/test/RecommendationsTest.php

(defn no-http-calls-fixture
  [test-fn]
  (let [fake-routes {}]
    (with-global-fake-routes-in-isolation fake-routes
      (test-fn))))

(use-fixtures :each no-http-calls-fixture)

;; ---

(def fixture-dir (-> "test/fixtures" fs/absolute fs/normalized str))

(defn fixture-path
  [filename]
  (if-let [r (io/resource (str "fixtures/" filename))]
    r
    (println filename "not found")))

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
          (let [result (core/recommendation-list "article" 1234 {:content-type-list [given-content-type]})]
            (is (= expected-content-type (:content-type-pair result)))))))))

(deftest recommendations-list--article-not-found
  (testing "failure to find an article skips any further lookups and passes the 'Not Found' response through."
    (let [expected (api-raml/http-error-response {:status 404})]
      (with-redefs [core/find-article (constantly expected)]
        (is (= expected (core/recommendation-list "article" 1234)))))))

(deftest recommendations-list--article-found-no-recommendations
  (testing "empty recommendations for an article that exists"
    (let [dummy-article {:content {:status 200}}
          empty-response {}
          expected {:content {:total 0
                              :items []}
                    :content-type (first api-raml/recommendations-list)
                    :content-type-version 2
                    :content-type-pair api-raml/recommendations-list-v2
                    :content-type-version-deprecated? false
                    :authenticated? false}]
      (with-redefs [core/find-article (constantly dummy-article)
                    core/find-related-articles (constantly empty-response)
                    core/find-collections (constantly empty-response)
                    core/find-articles-by-subject (constantly empty-response)
                    core/find-podcast-episodes (constantly empty-response)]
        (is (= expected (core/recommendation-list "article" 1234)))))))

(deftest recommendations-list--article-found-some-recommendations
  (testing "recommendations for an article that exists"
    (with-redefs [core/find-article (-> "find-article.json" fixture-path utils/slurp-json constantly)
                  core/find-related-articles (-> "find-related-articles.json" fixture-path utils/slurp-json constantly)
                  core/find-collections (-> "find-collections.json" fixture-path utils/slurp-json constantly)
                  core/find-articles-by-subject (-> "find-articles-by-subject.json" fixture-path utils/slurp-json constantly)
                  core/find-podcast-episodes (-> "find-podcast-episodes.json" fixture-path utils/slurp-json constantly)]
      (let [;; 79530
            expected (-> "recommendations-list.json" fixture-path utils/slurp-json)
            actual (core/recommendation-list "article" 1234)]

        (is (= expected actual))

        ))))
