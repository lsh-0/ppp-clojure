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
    (do
      (st/instrument)
      ;;(info "instrumentation is ON"))
      )
    (do
      (st/unstrument)
      ;;(info "instrumentation is OFF"))))
      )))

(defn spy
  [x]
  (println "spy:" x)
  x)

(defn slurp-json
  [input-path]
  ;;(println "slurping" (str input-path))
  (let [keywordise? true]
    (-> input-path slurp (json/parse-string keywordise?))))

(defn spit-json
  [edn output-fname]
  (let [output-path (fs/absolute output-fname)]
    (when true
      (spit output-path (json/generate-string edn {:pretty true})))
    (str output-path)))
