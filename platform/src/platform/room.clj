(ns platform.room)


(defprotocol IRoom
  "Interface Database Key Value"
  (room-host [database room-id owner-id] "Host a room")
  (room-join [database room-id user-id] "Join a room")
  (room-leave [database room-id] "Leave a room")
  (room-close! [database room-id] "Close a room"))
