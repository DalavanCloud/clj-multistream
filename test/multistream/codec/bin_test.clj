(ns multistream.codec.bin-test
  (:require
    [clojure.test :refer :all]
    [multistream.codec :as codec]
    [multistream.codec.bin :as bin]
    [multistream.header :as header]
    [multistream.test-utils :as tu])
  (:import
    (java.io
      ByteArrayInputStream
      ByteArrayOutputStream)))


(deftest binary-codec
  (let [codec (bin/bin-codec)
        content "foo bar baz"]
    (testing "processable headers"
      (is (codec/processable? codec "/bin/"))
      (is (not (codec/processable? codec "/text/"))))
    (let [baos (ByteArrayOutputStream.)]
      (header/write! baos "/bin/")
      (with-open [stream (codec/encode-byte-stream codec nil baos)]
        (is (satisfies? codec/EncoderStream stream))
        (is (= 11 (codec/write! stream (.getBytes content)))))
      (let [output-bytes (.toByteArray baos)]
        (is (= 18 (count output-bytes)))
        (let [input (ByteArrayInputStream. output-bytes)]
          (is (= (:header codec) (header/read! input)))
          (with-open [stream (codec/decode-byte-stream codec "/bin/" input)]
            (is (satisfies? codec/DecoderStream stream))
            (let [value (codec/read! stream)]
              (is (bytes? value))
              (is (= content (String. value))))))))
    (testing "eof behavior"
      (let [bais (ByteArrayInputStream. (byte-array 0))]
        (with-open [decoder (codec/decode-byte-stream codec nil bais)]
          (is (err-thrown? ::codec/eof (codec/read! decoder)))
          (binding [codec/*eof-guard* (Object.)]
            (is (identical? codec/*eof-guard* (codec/read! decoder)))))))))
