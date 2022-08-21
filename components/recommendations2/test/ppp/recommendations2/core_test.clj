(ns ppp.recommendations2.core-test
  (:require
   [clojure.test :as test :refer [deftest is testing]]
   [ppp.recommendations2.core :as core]
   [ppp.api-raml.interface :as api-raml]
   [clj-http.fake :refer [with-global-fake-routes-in-isolation]]
   ))

;; https://github.com/elifesciences/recommendations/blob/develop/test/RecommendationsTest.php

(deftest recommendations-list--article-not-found
  (testing "failure to find an article skips any further lookups and passed the 'Not Found' error through."
    (let [fake-routes {}
          expected (api-raml/http-error-response {:status 404})]
      (with-redefs [core/find-article (constantly expected)]
        (with-global-fake-routes-in-isolation fake-routes
          (is (= expected (core/recommendation-list "articles" "1234"))))))))

(deftest recommendations-list--article-found-no-recommendations
  (testing "it returns an empty recommendations for an article that exists"
    nil))
