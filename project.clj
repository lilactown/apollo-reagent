;; this project.clj is only used for releases.
;; shadow-cljs.edn should be treated as the source of truth
;; for building & testing
(defproject lilactown/apollo-reagent "0.0.1"
  :dependencies [[reagent "0.8.0"]
                 [funcool/promesa "1.9.0"]]
  :source-paths ["src"]
  :npm {:dependencies [[bluebird "3.5.1"]
                       [graphql "0.13.2"]
                       [graphql-tag "2.9.1"]]})
