(ns depsy.app
  (:require [reagent.core :as reagent :refer [atom]]
            [cljs.pprint :as pprint]
            [cljs.reader :as reader]))

(enable-console-print!)

(prn (cljs.reader/read-string "'[1 2 3]"))

(defn some-component []
      [:div
       [:h3 "I am a component!"]
       [:p.someclass
        "I have " [:strong "bold"]
        [:span {:style {:color "red"}} " and red"]
        " text."]])

(def example
  '[[org.clojure/clojure "1.9.0"]
    [org.clojure/clojurescript "1.10.339"]

    [adzerk/boot-cljs "2.0.0" :scope "test"]
    [adzerk/boot-cljs-repl "0.3.3" :scope "test"]
    [adzerk/boot-reload "0.6.0" :scope "test"]
    [pandeiro/boot-http "0.8.3" :scope "test"]
    [com.cemerick/piggieback "0.2.1" :scope "test"]
    [org.clojure/tools.nrepl "0.2.13" :scope "test"] ;; [nrepl "0.4.0"]
    [weasel "0.7.0" :scope "test"]

    [crisptrutski/boot-cljs-test "0.3.0" :scope "test"]
    [reagent "0.8.1"]
    [org.martinklepsch/boot-garden "1.3.2-0" :scope "test"]
    [binaryage/dirac "1.2.9" :scope "test"]
    [powerlaces/boot-cljs-devtools "0.2.0" :scope "test"]])

(defn e->v [event] (-> event .-target .-value))

(defn unquote [data]
      (prn "got:" data)
      (if (= 'quote (first data))
        (rest data)
        data))

(defn unquote-string
      "stupid function"
      [string]
      (if (= \' (first string))
        (rest string)
        string))

(defn textarea [value opts on-change]
      [:textarea (merge opts {:value     value
                              :rows      10
                              :cols      80
                              :style     {:width "100%"
                                          :height "100%"
                                          :font-size "16px"
                                          :font-family "Courier New, monospace"}
                              :on-change #(on-change (e->v %))})])

(defn xform-dep [[lib version & korks]]
      (let [args (into {} (apply hash-map korks))]
           [lib (merge {:mvn/version version} args)]))

(defn xform-deps
      "deps.edn utility function by @theronic 2018-02-28
         Transforms a collection of project.clj :dependencies to deps.edn style
       '[[org.clojure/clojurescript \"1.9.946\" :exclusions [org.clojure/clojure com.google.guava/guava]]
         ...]
        => {:deps {org.clojure/clojurescript {:mvn/version \" 1.9.946\", :exclusions [org.clojure/clojure com.google.guava/guava]}}
                  ...}"
      [coll]
      (binding [*print-namespace-maps* false
                pprint/*print-right-margin* 300]
        (let [converted (into {} (map xform-dep coll))]
          ;(prn "converted:" converted)
          ;(pprint/pprint converted)
          (with-out-str (pprint/pprint {:deps converted})))))


(defn prn* [x] ;;[& args]
      (prn x)
      x)
      ;;(apply prn args)
      ;args)

(defn deps-converter [initial-value]
      (let [!input (atom initial-value)                     ;; need a nice sample
            !error (atom nil)
            !output (atom "")                               ;; todo run
            on-change (fn [input]
                          (try
                            (->> input
                                 (reset! !input)
                                 (prn*)
                                 (unquote-string)
                                 (reader/read-string)
                                 ;(unquote)
                                 (prn*)
                                 (xform-deps)
                                 ;(pr-str)
                                 (reset! !output))
                            (reset! !error nil)
                            (catch :default ex
                              (reset! !error ex)
                              (js/console.error ex))))
            converted (on-change @!input)]
           (fn [_]
               [:div
                {:style {:display               "grid"
                         :height "100%"
                         :grid-column-gap       "3em"
                         :grid-template-columns "50% 50%"
                         :grid-template-rows   "\"auto auto\""
                         :grid-template-areas   "\"input output\""}}
                [:div
                 {:style {:grid-area "input"}}
                 [:h2 "Paste project.clj or build.boot :dependencies here:"]
                 [textarea @!input {} on-change]
                 [:p "Tip: unquote dependencies, so instead of '[1 2 3], paste [1 2 3]."]]
                [:div
                 {:style {:grid-area "output"}}
                 [:h2 [:code "deps.edn"] " dependencies output:"]
                 [textarea (if-let [err @!error]
                                   (pr-str err)
                                   @!output)
                  {:read-only true
                   :on-click #(.select (.-target %))} nil]
                 #_[:textarea {:value     (pr-str (or @!error @!output))
                               :rows      10
                               :cols      80
                               :style     {:width "100%"}
                               :read-only true}]]])))

(defn parent-component []
      [:div
       {:style {:font-family "Georgia"}}
       [:h1 "Depsy"
        " "
        [:small "A simple tool to translate "
         [:code "project.clj"]
         " or "
         [:code "build.boot"]
         " dependencies to " [:code "deps.edn"] " format."]]
       [deps-converter (with-out-str (pprint/pprint example))]
       [:div {:style {:clear "both"}}]
       [:hr]
       [:p "Made with love by " [:a {:href "http://petrustheron.com/"} "Petrus Theron"] ". You can " [:a {:href "https://github.com/theronic"} "read the source"] "."]])

(defn init []
      (reagent/render-component [parent-component]
                                (.getElementById js/document "container")))
