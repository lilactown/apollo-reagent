(ns apollo-reagent.server
  (:require [apollo-reagent.core :as ar]
            [reagent.ratom]
            [promesa.core :as p]
            [clojure.walk]))

(declare visit-children)

;; I don't think we ever use this
(defonce queries (atom []))

;; proxy so that it doesn't get rewritten later
(def watch-query! ar/watch-query!)

(defn reaction->promise [reaction]
  ;; use an atom to maintain the tracker so we can dispose it later
  (let [tracker (atom nil)]
    (p/promise
     (fn [resolve reject]
       ;; (println "promise-> making")
       (reset!
        tracker
        (reagent.ratom/track!
         (fn []
           (when (= (:loading @reaction) false)
             ;; (println "promise-> resolving")
             (resolve @reaction)
             (reagent.ratom/dispose! @tracker)))))))))

(defn track-query! [& args]
  ;; (println "tracking")
  (let [query (apply watch-query! args)]
    (swap! queries conj (reaction->promise query))
    query)
  )

(defn visit [node]
  (println "node:" node)
  (cond (and (vector? node) (fn? (first node)))
        (let [[component & props] node
              query-ln (count @queries)
              body (apply component props)]
          (if (> (count @queries) query-ln)
            (do (println "A query was added")
                (-> (p/all @queries)
                    (p/then #(do (println "promise-> " %)
                               (reset! queries [])
                               (visit-children body)))))
                )
            (visit-children body))
        :default node
        ))

(defn visit-children [body]
  (clojure.walk/prewalk
   visit
   body)
  (p/all @queries))

(defn preload [client component]
  (with-redefs [apollo-reagent.core/watch-query! track-query!
                apollo-reagent.server/queries (atom [])
                ]
    (let [vp (visit component)]
      (println "preloaded:" vp)
      (println "preloaded queries:" @queries)
      vp)
    ))
