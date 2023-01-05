(ns metabase.driver.common.parameters.parse
  (:require
   ;; [clojure.string :as str]
   [clojure.tools.logging :as log]
   [instaparse.core :as insta]
   [metabase.driver.common.parameters :as params]
   ;; [metabase.query-processor.error-type :as qp.error-type]
   [metabase.util :as u]
   ;; [metabase.util.i18n :refer [tru]]
   [schema.core :as s])
  (:import
   (metabase.driver.common.parameters Optional Param)))

(def ^:private sql-template-parser
  (insta/parser
   "TEMPLATE    = (TEXT | OPTIONAL | PARAM | SEPARATOR)*
    <TEXT>      = #'((?!\\{\\{|\\}\\}|\\[\\[|\\]\\]|--).)*'
    OPTIONAL    = <'[['> TEXT PARAM TEXT <']]'>
    PARAM       = <'{{'> <WHITESPACE> TOKEN <WHITESPACE> <'}}'>
    <SEPARATOR> = '\n' | COMMENT
    <COMMENT>   = #'--.*\n?'
    <TOKEN>     = #'\\w+'
    WHITESPACE  = #'\\s*'"))

(def ^:private transform-map
  {:PARAM    params/->Param
   :OPTIONAL (fn [& parsed] (params/->Optional parsed))})

(defn transform
  [parsed]
  (drop 1 (insta/transform transform-map parsed)))

(s/defn parse :- [(s/cond-pre s/Str Param Optional)]
  "Attempts to parse parameters in string `s`. Parses any optional clauses or parameters found, and returns a sequence
   of non-parameter string fragments (possibly) interposed with `Param` or `Optional` instances."
  [s :- s/Str]
  (let [parsed (insta/parse sql-template-parser s)]
    (log/tracef "Parsed native query ->\n%s" (u/pprint-to-str parsed))
    (u/prog1 (transform parsed)
             (log/tracef "Parsed native query ->\n%s" (u/pprint-to-str <>)))))

;; TODO add error handling...
