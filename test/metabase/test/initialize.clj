(ns metabase.test.initialize
  "Logic for initializing different components that need to be initialized when running tests."
  (:require [clojure.string :as str]
            [colorize.core :as colorize]
            [metabase.plugins.classloader :as classloader]))

(defmulti initialize-if-needed!
  "Initialize one or more components.

    (initialize-if-needed! :db :web-server)"
  (fn
    ([k]        (keyword k))
    ([k & more] :many)))

(defonce ^:private initialized (atom #{}))

(defn initialized?
  "Has this component been initialized?"
  ([k]
   (contains? @initialized k))

  ([k & more]
   (and (initialized? k)
        (apply initialized? more))))

(defmethod initialize-if-needed! :many
  [& args]
  (doseq [k args]
    (initialize-if-needed! k)))

(defn- log-init-message [task-name]
  (let [body   (format "| Initializing %s... |" task-name)
        border (str \+ (str/join (repeat (- (count body) 2) \-)) \+)]
    (println
     (colorize/blue
      (str "\n"
           (str/join "\n" [border body border])
           "\n")))))

(defmacro ^:private define-initialization [task-name & body]
  (let [delay-symb (vary-meta (symbol (format "init-%s-%d" (name task-name) (hash &form)))
                              assoc :private true)]
    `(do
       (defonce ~delay-symb
         (delay
           (log-init-message ~(keyword task-name))
           (swap! initialized conj ~(keyword task-name))
           ~@body
           nil))
       (defmethod initialize-if-needed! ~(keyword task-name)
         [~'_]
         @~delay-symb))))

(define-initialization :plugins
  (classloader/require 'metabase.test.initialize.plugins)
  ((resolve 'metabase.test.initialize.plugins/init!)))

;; initializing the DB also does setup needed so the scheduler will work correctly. (Remember that the scheduler uses
;; a JDBC backend!)
(define-initialization :db
  (classloader/require 'metabase.test.initialize.db)
  ((resolve 'metabase.test.initialize.db/init!)))

(define-initialization :web-server
  (initialize-if-needed! :db)
  (classloader/require 'metabase.test.initialize.web-server)
  ((resolve 'metabase.test.initialize.web-server/init!)))

(define-initialization :test-users
  (initialize-if-needed! :db)
  (classloader/require 'metabase.test.initialize.test-users)
  ((resolve 'metabase.test.initialize.test-users/init!)))

(define-initialization :test-users-personal-collections
  (initialize-if-needed! :test-users)
  (classloader/require 'metabase.test.initialize.test-users-personal-collections)
  ((resolve 'metabase.test.initialize.test-users-personal-collections/init!)))

(alter-meta! #'initialize-if-needed! assoc :arglists (list (into ['&] (sort (disj (set (keys (methods initialize-if-needed!)))
                                                                                  :many)))))
