# To run this code:
1. first needs to load the configuration files "Common.cfg" and "PeerInfo.cfg" from the remote server using the script. 
2. "make clean" and "make" would compile all the necessary java classes
3. java peerprocess [peerID]


# Architecture 
The main architecture of this project consists of the following main parts:
*PeerParser: Parse the information of each paper by reading the peerInfo.cfg file
*CommonParser: Same thing happens here. CommonParser read the info from common.cfg and save it into proper variables. 
*DataFile: First, the original file splits into chunks of byte arrays. When the peers are done with downloading all the pieces of data, the data chunks will be merged into a file and stored in the proper directory. 
PeerProcess: Configuration files will be parsed to extract all the necessary information. Then client object is initialized using the values just parsed. The new client will be made for each peer. 
*Client: Within the client class all the unchoking/choking timers have been set. Once the uploader class which acts as a          listener in this architecture would be called on a new thread. 
*Uploader: Uploader would listen on the TCP connection and pass the connection through the whole network.
*Connection: We need to track all the predefined connections to send the correct information toward the correct peer. All the different messages including piece, request, have, handshake, bitfield, interested and not interested have been implemented. 
  
