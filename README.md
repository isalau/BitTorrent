# BitTorrent
1. first needs to load the configuration files "Common.cfg" and "PeerInfo.cfg" from the remote server using the script. 
2. "make clean" and "make" would compile all the necessary java classes
3. The main architecture of this project consists of the following main parts:
  PeerProcess:Configuration files will be parsed to extract all the necessary information. Then client object is initialized using the values just parsed. New client will be made for each peer. 
  Client:Within the client class all the unchockin/chocing timers has been set. Once the uploader class which acts as a listener in this architecture would be called on a new thread. 
  Uploader:Uploader would listen on the TCP connection and pass the connection through the whole network.
  Connection:we need to track all the predefined conncetions to send the correct information toward the correct peer. All the different messages including piece, request, have, handshake, bitfield, interested and not interested have been implemented. 
  
