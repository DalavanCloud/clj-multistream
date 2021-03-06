(ns multistream.header
  "Functions for handling multicodec header paths.

  On error, these functions throw an `ExceptionInfo` with ex-data containing
  `:type :multistream.header/invalid` to indicate the problem. The data map will
  also usually have `:header` and `:length` entries."
  (:require
    [clojure.string :as str])
  (:import
    (java.io
      InputStream
      OutputStream)
    (java.nio.charset
      StandardCharsets)))


(def ^:no-doc ^:const max-header-length
  "The maximum length (in bytes) a header path can be."
  127)


(def ^:no-doc ^java.nio.charset.Charset header-charset
  "The character set that codec headers are encoded with."
  StandardCharsets/UTF_8)


(defn- bad-header-ex
  "Creates an exception for a bad header value. Any additional key-value pairs
  passed to the function will be included in the exception data."
  [message & {:as info}]
  (ex-info message (assoc info :type ::invalid)))



;; ## Header Encoding

(defn encode-header
  "Returns the byte-encoding of the header path. The path is trimmed and has a
  newline appended to it before encoding.

  Throws a bad-header exception if the path is too long."
  ^bytes
  [path]
  (let [header (str (str/trim path) "\n")
        header-bytes (.getBytes header header-charset)
        length (count header-bytes)]
    (when (> length max-header-length)
      (throw (bad-header-ex
               (format "Header paths longer than %d bytes are not supported: %d"
                       max-header-length length)
               :header header
               :length length)))
    (let [encoded (byte-array (inc length))]
      (aset-byte encoded 0 (byte length))
      (System/arraycopy header-bytes 0 encoded 1 length)
      encoded)))


(defn write!
  "Writes a multicodec header for `path` to the given stream. Returns the number
  of bytes written."
  [^OutputStream output path]
  (let [header (encode-header path)]
    (.write output header)
    (count header)))



;; ## Header Decoding

(defn- take-bytes!
  "Attempts to read `length` bytes from the given stream. Returns a byte array with
  the read bytes."
  ^bytes
  [^InputStream input length]
  (let [content (byte-array length)]
    (loop [offset 0
           remaining length]
      (if (pos? (.available input))
        (let [n (.read input content offset remaining)]
          (if (< n remaining)
            (recur (+ offset n) (- remaining n))
            content))
        content))))


(defn read!
  "Attempts to read a multicodec header from the given stream. Returns the
  header path.

  Throws a bad-header exception if the stream does not have a valid header."
  ^String
  [^InputStream input]
  (let [length (.read input)]
    (when-not (< length 128)
      (throw (bad-header-ex
               (format "First byte in stream is not a valid header length: %02x"
                       length)
               :length length)))
    (let [header (String. (take-bytes! input length) header-charset)]
      (when-not (.endsWith header "\n")
        (throw (bad-header-ex
                 (str "Last byte in header is not a newline: "
                      (pr-str (.charAt header (dec (count header)))))
                 :header header
                 :length length)))
      (str/trim-newline header))))
