(ns apollo-reagent.server
  (:require [apollo-reagent.core :as ar]
            [reagent.ratom]
            [promesa.core :as p]
            [clojure.walk]))

;; I don't think we ever use this
(defonce queries (atom []))

;; proxy so that it doesn't get rewritten later
(def watch-query! ar/watch-query!)

(defn reaction->promise
  "Converts a reagent Reaction to a promesa promise. The promise resolves once a
  value is emitted by the Reaction that `p` returns `true` for"
  [reaction p]
  ;; use an atom to maintain the tracker so we can dispose it later
  (let [tracker (atom nil)]
    (p/promise
     ;; TODO: Add logic to reject
     (fn [resolve _reject]
       ;; (println "promise-> making")
       (reset!
        tracker
        (reagent.ratom/track!
         (fn []
           (when (p @reaction)
             (println "promise-> resolving")
             (resolve @reaction)
             (reagent.ratom/dispose! @tracker)))))))))

(defn track-query!
  "Like `apollo-reagent.core/watch-query!`, but loads the query as a promesa
  promise into a global store for tracking."
  [& args]
  (let [query (apply watch-query! args)]
    (swap! queries conj (reaction->promise query #(= (:loading %) false)))
    query)
  )

(declare visit-tree)

(defn visit
  "Visits each node in the component tree. If the node is of the form
  `[fn prop1 ... propN], then we execute (fn prop1 ... propN)`.
  If a query was tracked after executing this, we await on all queries to
  resolve before walking the component tree returned by executing the component.

  Returns either a promesa promise or the node itself."
  [node]
  ;; (println "node:" node)
  (cond (and (vector? node) (fn? (first node)))
        (let [[component & props] node
              query-ln (count @queries)
              body (apply component props)]
          (if (> (count @queries) query-ln)
            (do (println "A query was added")
                (-> (p/all @queries)
                    (p/then #(do ;; (println "promise-> " %)
                               (reset! queries [])
                               (visit-tree body)))))
                )
            (visit-tree body))
        :default node
        ))

(defn visit-tree
  "Walks the tree. Returns a promise that resolves when all tracked queries that
  were created by visiting resolve"
  [body]
  (clojure.walk/prewalk
   visit
   body)
  (p/all @queries))

(defn preload
  "Takes a reagent component tree and fetches any apollo-reagent queries. Returns
  a promesa promise that resolves when all queries are done loading.
  NOTE: Also does any other side effects that happen when executing your
  components"
  [tree]
  (with-redefs [apollo-reagent.core/watch-query! track-query!
                apollo-reagent.server/queries (atom [])]
    (visit-tree tree)
    ))
