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

(defn visit [component props]
  (if-not (fn? component)
    (visit-children props)
    (let [query-ln (count @queries)
          instance (apply component props)]
      (if (> (count @queries) query-ln)
        (do (println "A query was added")
            (-> (p/all @queries)
                (p/then #(do ;; (println "promise-> " %)
                           (reset! queries [])
                           (if (fn? instance)
                             ;; form-2 component
                             (visit-children (instance props))
                             (visit-children instance)
                             ))))
            )
        ;; no query was added
         (if (fn? instance)
           ;; form-2 component
           (visit-children (instance props))
           (visit-children instance))))
      )
  )

(defn visit-children [instance]
  ;; (println instance)
  (let [v (clojure.walk/prewalk
           (fn [form]
             ;; (println form)
             (if (and (vector? form)
                      (fn? (first form)))
               (visit (first form) (rest form))
               (do ;; (println form)
                   form)))
           instance)]
  (js/console.log v)
  (p/all v)))

(defn preload [client component props]
    (with-redefs [apollo-reagent.core/watch-query! track-query!
                  apollo-reagent.server/queries (atom [])
                  ]
      (let [vp (visit component props)]
      (println @queries)
      vp)
      ))
