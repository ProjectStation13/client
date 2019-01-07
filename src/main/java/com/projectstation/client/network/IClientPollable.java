package com.projectstation.client.network;

import com.projectstation.network.IServerVisit;
import com.projectstation.network.NetworkPollException;

import java.util.List;

public interface IClientPollable {
    List<IServerVisit> poll(int deltaTime) throws NetworkPollException;
}
