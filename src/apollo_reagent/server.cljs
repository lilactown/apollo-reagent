(ns apollo-reagent.server
  (:require [apollo-reagent.core :as ar]
            [clojure.walk]))

(declare visit-children)

;; I don't think we ever use this
(defonce queries (atom []))

(defn track-query! [client query config]
  (println query)
  (swap! queries conj {:instance instance
                       :client client
                       :query query
                       :config config})
  (atom nil))

(defn visit [component props]
  (let [query-ln (count @queries)
        instance (apply component props)]
    (if (> (count @queries) query-ln)
      (do (println "A query was added")
          (if (fn? instance)
            ;; form-2 component
            (visit-children (instance props))
            (visit-children instance)))
      ;; no query was added
      (if (fn? instance)
        ;; form-2 component
        (visit-children (instance props))
        (visit-children instance)))
    ))

(defn visit-children [instance]
  (clojure.walk/prewalk
   (fn [form]
     ;; (println form)
     (if (and (vector? form)
          (fn? (first form)))
       (visit (first form) (rest form))
       form))
   instance))

(defn preload [client component props]
    (with-redefs [apollo-reagent.core/watch-query! track-query!
                  apollo-reagent.server/queries (atom [])
                  ]
      (visit component props)
      (println @queries)))
