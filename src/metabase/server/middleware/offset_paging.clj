(ns metabase.server.middleware.offset-paging
  (:require
   [medley.core :as m]
   [metabase.util.i18n :refer [tru]]))

(def ^:dynamic *limit* "Limit for offset-limit paging." nil)
(def ^:private default-limit 50)

(def ^:dynamic *offset* "Offset for offset-limit paging." nil)
(def ^:private default-offset 0)

(def ^:dynamic *paged?*
  "Bool for whether a request is paged or not.
  Automatically generated by a handler in offset-paging middleware."
  false)

(defn- offset-paged? [{{:strs [page limit offset]} :query-params}]
  (or page limit offset))

(defn- parse-paging-params [{{:strs [limit offset]} :query-params}]
  (let [limit  (or (some-> limit Integer/parseUnsignedInt)
                   default-limit)
        offset (or (some-> offset Integer/parseUnsignedInt)
                   default-offset)]
    {:limit limit, :offset offset}))

(defn- with-paging-params [request {:keys [limit offset]}]
  (-> request
      (assoc ::limit limit, ::offset offset)
      (m/dissoc-in [:query-params "offset"])
      (m/dissoc-in [:query-params "limit"])
      (m/dissoc-in [:params :offset])
      (m/dissoc-in [:params :limit])))

(defn handle-paging
  "Limit offset paging.
  This has many downsides but many upsides, chief among them at-will random paging.
  (it isn't stable with respect to underlying data changing, though)"
  [handler]
  (fn [request respond raise]
    (if-not (offset-paged? request)
      (handler request respond raise)
      (let [paging-params (try
                            (parse-paging-params request)
                            (catch Throwable e
                              e))]
        (if (instance? Throwable paging-params)
          (raise (ex-info (tru "Error parsing paging parameters: {0}" (ex-message paging-params))
                          {:status-code 400}))
          (let [{:keys [limit offset]} paging-params
                request                (with-paging-params request paging-params)]
            (binding [*limit*  limit
                      *offset* offset
                      *paged?* true]
              (handler request respond raise))))))))
