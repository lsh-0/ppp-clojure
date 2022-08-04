(ns ppp.cli.core
  (:require
   [ppp.cli.api :as api])
  (:gen-class))

(defn -main [& args]
  (println "hello, goodbye")
  (System/exit 0))
