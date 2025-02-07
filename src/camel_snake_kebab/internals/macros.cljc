(ns ^:no-doc camel-snake-kebab.internals.macros
  #?(:cljs (:refer-clojure :exclude [resolve]))
  (:require [camel-snake-kebab.internals.alter-name :refer [alter-name]]
            [camel-snake-kebab.internals.misc :refer [convert-case]]))

#?(:cljs
   (defn resolve [sym]
     ;; On self-hosted ClojureScript, macros are evaluated under the `:cljs` conditional branch
     ;; In that case, we need to use `eval` in order to resolve variables instead of `resolve`
     (eval `(~'var ~sym))))

(defn type-preserving-function [case-label first-fn rest-fn sep]
  `(defn ~(symbol (str "->" case-label)) [s# & rest#]
     (let [convert-case# #(apply convert-case ~first-fn ~rest-fn ~sep % rest#)]
       (alter-name s# convert-case#))))

(defn type-converting-functions [case-label first-fn rest-fn sep]
  (letfn [(make-name [type-label]
            (->> (str case-label " " type-label)
                 (convert-case (resolve first-fn) (resolve rest-fn) sep)
                 (str "->")
                 (symbol)))]
    (for [[type-label type-converter] {"string" `identity "symbol" `symbol "keyword" `keyword}]
      `(defn ~(make-name type-label) [s# & rest#]
         (if (integer? s#)
           s#
           (~type-converter (apply convert-case ~first-fn ~rest-fn ~sep (name s#) rest#)))))))

(defmacro defconversion [case-label first-fn rest-fn sep]
  `(do  ~(type-preserving-function  case-label first-fn rest-fn sep)
       ~@(type-converting-functions case-label first-fn rest-fn sep)))
