(ns apollo-reagent.core
  (:require [reagent.core]
            [reagent.ratom]
            ["graphql-tag" :as gql]))

(defn create-client [client options]
  (client. (clj->js options)))

(defn query! [client query config]
  (.query client
          (-> {:query (gql query)}
              (merge config)
              (clj->js))))

(defn watch-query! [client query config]
  (let [observable-query (.watchQuery
                          client
                          (clj->js (-> {:query (gql query)}
                                       (merge config)
                                       (clj->js))))
        result-ratom (reagent.ratom/atom
                      (js->clj (.currentResult observable-query)
                               :keywordize-keys true))
        ;; TODO: refactor this so that subscription is accessible
        ;; so that we can call e.g. refetch
        subscription (.subscribe
                      observable-query
                      (clj->js
                       {:next (fn [result]
                                (reset!
                                 result-ratom
                                 (js->clj result :keywordize-keys true)))}))]
    ;; we use a custom reaction so we can unsubscribe from the observable
    ;; on dispose
    (reagent.ratom/make-reaction
     (fn [] @result-ratom)
     :on-dispose #(do (.unsubscribe subscription)
                      ;; I guess ratom's don't need to be disposed? this throws
                      ;; (reagent.ratom/dispose! result-ratom)
                      )
     ;; I don't know what this does so I'm commenting it out for now
     ;; :auto-run true
     )
    ))

(defn mutate! [client mutation config] (println "STUB"))

(defn read-query [client query config]
  (.readyQuery client (-> {:query (gql query)}
                          (merge config)
                          (clj->js))))

(defn write-query! [client query config]
  (println "STUB"))

(defn read-fragment [client fragment config]
  (println "STUB"))

(defn write-fragment! [client fragment config]
  (println "STUB"))

(defn reset-store! [client]
  (.resetStore client))
