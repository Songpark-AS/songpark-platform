(ns platform.oauth)


(defprotocol IOAuth
  (get-request [provider state])
  (fetch-token [provider state oauth2-resp])
  (fetch-data [provider token] [provider token params] [provider token path params]))
