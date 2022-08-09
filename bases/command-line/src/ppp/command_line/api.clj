(ns ppp.command-line.api
  (:require
   [ppp.lax.interface :as lax]))

(run! (fn [[ns var-list]]
        (let [ns-intern-map (ns-interns ns)]
          (run! (fn [v]
                  (if-let [vr (get ns-intern-map v)]
                    (intern 'ppp.command-line.api v vr)
                    (println "failed to find" v "in" ns-intern-map))) var-list)))

      {'ppp.lax.interface ['article 'article-list 'related-article-list 'article-version-list 'article-version]

       })
