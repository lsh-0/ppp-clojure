(ns ppp.cli.core
  (:require
   [ppp.user.interface :as user])
  (:gen-class))

(defn foo
  [bar]
  (println "baz" bar))

(defn -main [& args]
  (println (user/hello (first args)))
  (System/exit 0))
