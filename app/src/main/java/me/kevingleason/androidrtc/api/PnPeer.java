package me.kevingleason.androidrtc.api;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.DataChannel;
import org.webrtc.IceCandidate;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.SdpObserver;
import org.webrtc.SessionDescription;

/**
 * Created by GleasonK on 7/22/15.
 */
public class PnPeer implements SdpObserver, PeerConnection.Observer {
    public static final String TAG = "PnPeer";
    public static final String STATUS_INIT         = "INITIALIZING";
    public static final String STATUS_CONNECTING   = "CONNECTING";
    public static final String STATUS_CONNECTED    = "CONNECTED"; // TODO: Where to change status to this?
    public static final String STATUS_DISCONNECTED = "DISCONNECTED";
    public static final String TYPE_NONE           = "NONE";
    public static final String TYPE_OFFER          = "offer";
    public static final String TYPE_ANSWER         = "answer";

    private PnPeerConnectionClient pcClient;
    PeerConnection pc;
    String id;
    String type;
    String status;
    boolean dialed;
    boolean received;
//        private int endPoint;
    // Todo: Maybe attach MediaStream as private var?

    public PnPeer(String id, PnPeerConnectionClient pcClient) {
        Log.d(TAG, "new Peer: " + id);
        this.id = id;
        this.type = TYPE_NONE;
        this.setStatus(STATUS_INIT);
        this.dialed = false;
        this.received = false;
        this.pcClient = pcClient;
        this.pc = pcClient.pcFactory.createPeerConnection(pcClient.signalingParams.iceServers,
                pcClient.signalingParams.pcConstraints, this);
        setStatus(STATUS_CONNECTING);
        pc.addStream(pcClient.getLocalMediaStream());
        pcClient.mRtcListener.onStatusChanged(this);
    }

    public void setStatus(String status){
        this.status = status;
    }

    public String getStatus() {
        return status;
    }

    public void setType(String type){this.type = type;}

    public String getType() {
        return type;
    }

    public boolean isDialed() {
        return dialed;
    }

    public void setDialed(boolean dialed) {
        this.dialed = dialed;
    }

    public boolean isReceived() {
        return received;
    }

    public void setReceived(boolean received) {
        this.received = received;
    }

    public PeerConnection getPc() {
        return pc;
    }

    public String getId() {
        return id;
    }


    @Override
    public void onCreateSuccess(final SessionDescription sdp) {
        // TODO: modify sdp to use pcParams prefered codecs
        try {
            JSONObject payload = new JSONObject();
            payload.put("type", sdp.type.canonicalForm());
            payload.put("sdp", sdp.description);
            pcClient.transmitMessage(id, payload);
            pc.setLocalDescription(PnPeer.this, sdp);

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onSetSuccess() {
    }

    @Override
    public void onCreateFailure(String s) {
    }

    @Override
    public void onSetFailure(String s) {
    }

    @Override
    public void onSignalingChange(PeerConnection.SignalingState signalingState) {
    }

    @Override
    public void onIceConnectionChange(PeerConnection.IceConnectionState iceConnectionState) {
        if (iceConnectionState == PeerConnection.IceConnectionState.DISCONNECTED) {
            pcClient.removePeer(id); // Should I remove? TODO: Ponder.
            setStatus(STATUS_DISCONNECTED);
            pcClient.mRtcListener.onStatusChanged(PnPeer.this);
        }
    }

    @Override
    public void onIceGatheringChange(PeerConnection.IceGatheringState iceGatheringState) {
    }

    @Override
    public void onIceCandidate(final IceCandidate candidate) {
        try {
            JSONObject payload = new JSONObject();
            payload.put("sdpMLineIndex", candidate.sdpMLineIndex);
            payload.put("sdpMid", candidate.sdpMid);
            payload.put("candidate", candidate.sdp);
            pcClient.transmitMessage(id, payload);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onAddStream(MediaStream mediaStream) {
        Log.d(TAG, "onAddStream " + mediaStream.label());
        // remote streams are displayed from 1 to MAX_PEER (0 is localStream)
        pcClient.mRtcListener.onAddRemoteStream(mediaStream, PnPeer.this);
    }

    @Override
    public void onRemoveStream(MediaStream mediaStream) {
        Log.d(TAG, "onRemoveStream " + mediaStream.label());
        PnPeer peer = pcClient.removePeer(id);
        pcClient.mRtcListener.onRemoveRemoteStream(mediaStream, peer);
    }

    @Override
    public void onDataChannel(DataChannel dataChannel) {
    }

    @Override
    public void onRenegotiationNeeded() {

    }

    /**
     * Overriding toString for debugging purposes.
     * @return String representation of a peer.
     */
    @Override
    public String toString(){
        return this.id + " Status: " + this.status + " Dialed: " + this.dialed +
                " Received: " + this.received + " Type: " + this.type;
    }

}
