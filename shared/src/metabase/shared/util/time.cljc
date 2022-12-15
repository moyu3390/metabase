(ns metabase.shared.util.time
  "Time parsing helper functions.
  In Java these return [[OffsetDateTime]], in JavaScript they return Moments."
  (:require
   [clojure.string :as str]
   [metabase.shared.util :as shared.u]
   #?@(:clj  [[java-time :as t]
              [metabase.public-settings :as public-settings]]
       :cljs [["moment" :as moment]]))
  #?(:clj (:import
           java.util.Locale)))

(defn- now []
  #?(:cljs (moment) :clj (t/offset-date-time)))

(defn- by-unit [_ {:keys [unit]}] unit)

(defn valid-datetime?
  "Given a platform-specific datetime, checks that it's valid. Non-nil in JVM, Moment.isValid in JS."
  [value]
  #?(:cljs (.isValid ^moment/Moment value) :clj (boolean value)))

(defmulti to-range
  "Given a datetime and a unit (eg. \"hour\"), returns an *inclusive* datetime range as a pair of datetimes.
  For a unit of an hour, and a datetime for 13:49:28, that means [13:00:00 13:59:59.999], ie. 1 ms before the end."
  by-unit)

(defmethod to-range :default [value {:keys [unit]}]
  #?(:cljs (let [^moment/Moment value value
                 ^moment/Moment c1    (.clone value)
                 ^moment/Moment c2    (.clone value)]
             [(.startOf c1 unit)
              (.endOf   c2 unit)])
     :clj  (throw (ex-info "to-range not defined for unit" {:unit unit :value value}))))

(defn first-day-of-week
  "The first day of the week varies by locale, but Metabase has a setting that overrides it.
  In CLJS, Moment knows the week specifier.
  In CLJ, check the setting directly."
  []
  #?(:cljs (-> (moment/weekdays 0)
               (.toLowerCase)
               keyword)
     :clj  (public-settings/start-of-week)))

;; The :default to-range works for nearly every unit in CLJS, but CLJ needs custom handling.
#?(:clj
   (do (defn- minus-ms [value]
         (t/minus value (t/millis 1)))

       (defmethod to-range :default [value _]
         ;; Fallback: Just return a zero-width at the input time.
         ;; This mimics Moment.js behavior if you `m.startOf("unknown unit")` - it doesn't change anything.
         [value value])
       (defmethod to-range "minute" [value _]
         (let [start (t/truncate-to value :minutes)]
           [start (minus-ms (t/plus start (t/minutes 1)))]))
       (defmethod to-range "hour" [value _]
         (let [start (t/truncate-to value :hours)]
           [start (minus-ms (t/plus start (t/hours 1)))]))
       (defmethod to-range "day" [value _]
         (let [start (t/truncate-to value :days)]
           [start (minus-ms (t/plus start (t/days 1)))]))
       (defmethod to-range "week" [value _]
         (let [first-day (first-day-of-week)
               start (-> value
                         (t/truncate-to :days)
                         (t/adjust :previous-or-same-day-of-week first-day))]
           [start (minus-ms (t/plus start (t/weeks 1)))]))
       (defmethod to-range "month" [value _]
         (let [value (t/truncate-to value :days)]
           [(t/adjust value :first-day-of-month)
            (minus-ms (t/adjust value :first-day-of-next-month))]))
       (defmethod to-range "year" [value _]
         (let [value (t/truncate-to value :days)]
           [(t/adjust value :first-day-of-year)
            (minus-ms (t/adjust value :first-day-of-next-year))]))))

(defmulti ^:private parse-timestamp-text by-unit)

(defmethod parse-timestamp-text :default [value _]
  ;; Best effort to parse this unknown string format, as a local zoneless datetime, then treating it as UTC.
  #?(:cljs (moment/utc value)
     :clj  (let [base (try (t/local-date-time value)
                           (catch Exception _
                             (try (t/local-date value)
                                  (catch Exception _
                                    nil))))]
             (when base
               (t/offset-date-time base (t/zone-id))))))

(defmethod parse-timestamp-text "day-of-week" [value options]
  ;; Try to parse as a regular timestamp; if that fails then try to treat it as a weekday name and adjust from
  ;; the current time.
  (let [as-default (try ((get-method parse-timestamp-text :default) value options)
                        (catch #?(:clj Exception :cljs js/Error) _
                          nil))]
    (if (valid-datetime? as-default)
      as-default
      #?(:cljs (-> (now)
                   (.isoWeekday value)
                   (.startOf "day"))
         :clj  (let [day (try (t/day-of-week "EEE" value)
                              (catch Exception _
                                (t/day-of-week "EEEE" value)))]
                 (-> (now)
                     (t/truncate-to :days)
                     (t/adjust :previous-or-same-day-of-week :monday)  ; Move to ISO start of week.
                     (t/adjust :next-or-same-day-of-week day)))))))    ; Then to the specified day.

(defmulti ^:private parse-timestamp-number by-unit)

(defmethod parse-timestamp-number :default [value _]
  ;; If no unit is given, or the unit is not recognized, try to parse the number as year number, returning the timestamp
  ;; for midnight UTC on January 1.
  #?(:cljs (moment/utc value moment/ISO_8601)
     :clj  (t/offset-date-time value)))

(defmethod parse-timestamp-number "minute-of-hour" [value _]
  #?(:cljs (.. (now) (minute value) (startOf "minute"))
     :clj  (-> (now) (t/truncate-to :hours) (t/plus (t/minutes value)))))

(defmethod parse-timestamp-number "hour-of-day" [value _]
  #?(:cljs (.. (now) (hour value) (startOf "hour"))
     :clj  (-> (now) (t/truncate-to :days) (t/plus (t/hours value)))))

(defmethod parse-timestamp-number "day-of-week" [value _]
  ;; Metabase uses 1 to mean the start of the week, based on the Metabase setting for the first day of the week.
  ;; Moment uses 0 as the first day of the week in its configured locale.
  ;; For Java, get the first day of the week from the setting, and offset by `(dec value)` for the current day.
  #?(:cljs (.. (now) (weekday (dec value)) (startOf "day"))
     :clj  (-> (now)
               (t/adjust :previous-or-same-day-of-week (first-day-of-week))
               (t/truncate-to :days)
               (t/plus (t/days (dec value))))))

(defn- magic-base-date
  "For some of these calculations, we need a date that is (a) in a month with 31 days,(b) in a leap year.
  This uses 2016-01-01 for the purpose. This is a function that returns fresh values since Moment is mutable."
  []
  #?(:cljs (moment "2016-01-01")
     :clj  (t/offset-date-time 2016 01 01)))

(defmethod parse-timestamp-number "day-of-month" [value _]
  ;; We force the initial date to be in a month with 31 days.
  #?(:cljs (.. (magic-base-date) (date value) (startOf "day"))
     :clj  (-> (magic-base-date)
               (t/plus (t/days (dec value))))))

(defmethod parse-timestamp-number "day-of-year" [value _]
  ;; We force the initial date to be in a leap year (2016).
  #?(:cljs (.. (magic-base-date) (dayOfYear value) (startOf "day"))
     :clj  (-> (magic-base-date)
               (t/plus (t/days (dec value))))))

#?(:cljs (defmethod parse-timestamp-number "week-of-year" [value _]
           (.. (now) (week value) (startOf "week")))
   :clj  (defmethod parse-timestamp-number "week-of-year" [value _]
           (-> (now)
               (t/truncate-to :days)
               (t/adjust :first-day-of-year)
               (t/adjust :previous-or-same-day-of-week (first-day-of-week))
               (t/plus (t/weeks (dec value))))))

(defmethod parse-timestamp-number "month-of-year" [value _]
  #?(:cljs (.. (now) (month (dec value)) (startOf "month"))
     :clj  (t/offset-date-time (t/year (now)) value 1)))

(defmethod parse-timestamp-number "quarter-of-year" [value _]
  #?(:cljs (.. (now) (quarter value) (startOf "quarter"))
     :clj  (let [month (inc (* 3 (dec value)))]
             (t/offset-date-time (t/year (now)) month 1))))

(defmethod parse-timestamp-number "year" [value _]
  #?(:cljs (.. (now) (year value) (startOf "year"))
     :clj  (t/offset-date-time value 1 1)))

(def ^:private default-options
  (-> {}
      #?(:clj (assoc :locale (Locale/getDefault)))))

(defn- prep-options [options]
  (merge default-options (shared.u/normalize-map options)))

(defn ^:export parse-timestamp
  "Parses a timestamp value into a date object. This can be a straightforward Unix timestamp or ISO format string.
  But the `:unit` field can be used to alter the parsing to, for example, treat the input number as a day-of-week or
  day-of-month number.
  Returns Moments in JS and OffsetDateTimes in Java."
  ([value] (parse-timestamp value {}))
  ([value options]
   (let [options (prep-options options)
         base (cond
                ;; Just return an already-parsed value. (Moment in CLJS, DateTime classes in CLJ.)
                #?@(:cljs [(moment/isMoment value) value]
                    :clj  [(or (t/offset-date-time? value)
                               (t/zoned-date-time? value)
                               (t/instant? value))
                           (t/offset-date-time value)])
                ;; If there's a timezone offset, or Z for Zulu/UTC time, parse it directly.
                (and (string? value) (re-matches #".*(Z|[+-]\d\d:?\d\d)$" value))
                #?(:cljs (moment/parseZone value)
                   :clj  (t/offset-date-time value))
                ;; Then we fall back to two multimethods for parsing based on different formats, per the unit.
                (string? value) (parse-timestamp-text value options)
                :else           (parse-timestamp-number value options))]
     (if (:local options)
       #?(:cljs (.local base)
          :clj  (t/local-date-time base))
       base))))

#?(:cljs
   (def ^:private parse-time-formats
     #js ["HH:mm:ss.SSS[Z]"
          "HH:mm:ss.SSS"
          "HH:mm:ss"
          "HH:mm"]))

(defn- parse-time-string [value]
  (let [plus (str/index-of value "+")
        value (if plus
                (subs value 0 plus)
                value)]
    #?(:cljs (moment value parse-time-formats)
       :clj  (t/local-time value))))

(defn ^:export parse-time
  "Parses a standalone time, or the time portion of a timestamp.
  Accepts a platform time value (eg. Moment, OffsetDateTime, LocalTime) or a string."
  [value]
  (cond
    #?@(:cljs [(moment/isMoment value) value]
        :clj  [(t/local-time? value)   value])
    (string? value) (parse-time-string value)
    :else           (throw (ex-info "Unknown input to parse-time; expecting a string"
                                    {:value value}))))

(defn same-day?
  "Given two platform-specific datetimes, checks if they fall within the same day."
  [d1 d2]
  #?(:cljs (.isSame ^moment/Moment d1 d2 "day")
     :clj  (= (t/truncate-to d1 :days) (t/truncate-to d2 :days))))

(defn same-month?
  "True if these two datetimes fall in the same (year and) month."
  [d1 d2]
  (let [f #?(:cljs #(.month ^moment/Moment %)
             :clj  t/month)]
    (= (f d1) (f d2))))

(defn same-year?
  "True if these two datetimes fall in the same year."
  [d1 d2]
  (let [f #?(:cljs #(.year ^moment/Moment %) :clj t/year)]
    (= (f d1) (f d2))))
