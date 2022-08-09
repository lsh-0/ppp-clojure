(ns ppp.cli.core
  (:require
   [ppp.cli.api :as api])
  (:gen-class))

(defn known-commands [] (->> 'ppp.cli.api ns-interns keys (mapv str)))

(defn find-exec
  [arg-list]
  (let [cmd (some-> arg-list first (clojure.string/split #"/") last)
        fn-name (str "ppp.cli.api/" cmd) ;; use full path, not alias
        kwargs {}
        fn-args (conj (vec (rest arg-list)) kwargs)]
    (if-let [api-fn (resolve (symbol fn-name))]
      (apply api-fn fn-args)
      (do (println "command not found:" cmd)
          (println "known commands:" (clojure.string/join ", " (known-commands)))))))

(defn -main
  [& args]
  (println (find-exec args))
  (System/exit 0))
