(ns ppp.utils.interface
  (:require
   [orchestra.spec.test :as st]
   [me.raynes.fs :as fs]
   [cheshire.core :as json]
   ))

(defn instrument
  "if `flag` is true, enables spec checking instrumentation, otherwise disables it."
  [flag]
  (if flag
    (st/instrument)
    (st/unstrument)))

(defn spy
  [x]
  (println "spy:" x)
  x)

(defn slurp-json
  [input-path]
  (let [keywordise? true]
    (-> input-path slurp (json/parse-string keywordise?))))

(defn spit-json
  [edn output-fname]
  (let [output-path (fs/absolute output-fname)]
    (when false
      (spit output-path (json/generate-string edn {:pretty true})))
    (str output-path)))

(defn string-to-boolean
  [string]
  (try
    (Boolean/valueOf string)
    (catch IllegalArgumentException exc
      false)))

;; https://github.com/CircleCI-Archived/frontend/blob/04701bd314731b6e2a75c40085d13471b696c939/src-cljs/frontend/utils.cljs
(defn deep-merge* [& maps]
  (let [f (fn [old new]
            (if (and (map? old) (map? new))
              (merge-with deep-merge* old new)
              new))]
    (if (every? map? maps)
      (apply merge-with f maps)
      (last maps))))

(defn deep-merge
  "Merge nested maps. At each level maps are merged left to right. When all
  maps have a common key whose value is also a map, those maps are merged
  recursively. If any of the values are not a map then the value from the
  right-most map is chosen.
  E.g.:
  user=> (deep-merge {:a {:b 1}} {:a {:c 3}})
  {:a {:c 3, :b 1}}
  user=> (deep-merge {:a {:b 1}} {:a {:b 2}})
  {:a {:b 2}}
  user=> (deep-merge {:a {:b 1}} {:a {:b {:c 4}}})
  {:a {:b {:c 4}}}
  user=> (deep-merge {:a {:b {:c 1}}} {:a {:b {:e 2 :c 15} :f 3}})
  {:a {:f 3, :b {:e 2, :c 15}}}
  Each of the arguments to this fn must be maps:
  user=> (deep-merge {:a 1} [1 2])
  AssertionError Assert failed: (and (map? m) (every? map? ms))
  Like merge, a key that maps to nil will override the same key in an earlier
  map that maps to a non-nil value:
  user=> (deep-merge {:a {:b {:c 1}, :d {:e 2}}}
                     {:a {:b nil, :d {:f 3}}})
  {:a {:b nil, :d {:f 3, :e 2}}}"
  [& maps]
  (let [maps (filter identity maps)]
    (assert (every? map? maps))
    (apply merge-with deep-merge* maps)))
