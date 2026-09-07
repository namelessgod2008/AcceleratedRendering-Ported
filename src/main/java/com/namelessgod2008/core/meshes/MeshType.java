package com.namelessgod2008.core.meshes;

import lombok.Getter;

@Getter
public enum MeshType {

	SERVER(ServerMesh.Builder.INSTANCE),
	CLIENT(ClientMesh.Builder.INSTANCE);

	private final IMesh.Builder builder;

	MeshType(IMesh.Builder builder) {
		this.builder = builder;
	}
}
