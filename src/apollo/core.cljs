(ns apollo.core
  (:require [reagent.core]
            [apollo-reagent.core :as ar]
            [apollo-reagent.server :as ars]
            ["apollo-boost" :as apollo-boost]))

;; shadow-cljs' ES modules don't work for this for some reason
(defonce client (ar/create-client apollo-boost/default
                               {:uri "http://localhost:3000"}))

(defn poke-name [number]
  (let [pokemon (ar/watch-query!
                 client
                 "query NameWeight($number: Int!) { pokemon(number: $number) { name  } }"
                 {:variables {:number number}})]
    (fn [number]
      (js/console.log @pokemon)
      (let [{:keys [name weight]} (get-in @pokemon [:data :pokemon])]
        [:div "Pokemon " number "'s name is " name]))))

(defn poke-name-weight [number]
  (let [pokemon (ar/watch-query!
                 client
                 "query NameWeight($number: Int!) { pokemon(number: $number) { name weight } }"
                 {:variables {:number number}})]
    (fn [number]
      (js/console.log @pokemon)
      (let [{:keys [name weight]} (get-in @pokemon [:data :pokemon])]
        [:<>
         [:div "Pokemon " number "'s name is " name]
         [:div "And weight is " weight]]))))


(defn page []
  [:div [poke-name 5]
   [poke-name-weight 4]])

(defn start []
  (reagent.core/render-component [page]
                                 (. js/document (getElementById "app")))
  (ars/preload nil page nil))

(defn ^:export init []
  ;; init is called ONCE when the page loads
  ;; this is called in the index.html and must be exported
  ;; so it is available even in :advanced release builds
  (start))

(defn stop []
  ;; stop is called before any code is reloaded
  ;; this is controlled by :before-load in the config
  (js/console.log "stop"))
