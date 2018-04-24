# apollo-reagent

```clojure
(ns my-app.core
  (:require [reagent.core :as r]
            [apollo-reagent.core :as ar]
            ["apollo-boost" :as apollo-client]))

;; Create an apollo-client. Here, we use the apollo-boost imported lib from npm.
;; shadow-cljs' ES modules don't work for this for some reason
(defonce client (ar/create-client apollo-boost/default
                               {:uri "http://localhost:3000"}))

;; A reagent component that will reactively render
;; when data for a GraphQL query changes
(defn poke-name [number]
  (let [pokemon (ar/watch-query!
                 client
                 "query NameWeight($number: Int!) { pokemon(number: $number) { name  } }"
                 {:variables {:number number}})]
      (if (:loading @pokemon)
        [:div "Loading"]

        ;; we have data
        (let [{:keys [name weight]} (get-in @pokemon [:data :pokemon])]
          [:div "Pokemon " number "'s name is " name]))))

(defn app []
  [:div [poke-name 200]
        ;; show the original 150
        (for [n (range 150)]
          {^:key n} [poke-name n])])
        
(reagent.core/render-component [app]
                               (. js/document (getElementById "app")))
```

## Run

``` shell
yarn install

yarn watch
```

## Clean

``` shell
yarn clean
```

## Release

``` shell
yarn release
```

## License

Copyright © 2017 FIXME

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
