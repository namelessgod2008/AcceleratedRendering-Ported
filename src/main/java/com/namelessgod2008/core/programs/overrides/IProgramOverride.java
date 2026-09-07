package com.namelessgod2008.core.programs.overrides;

public sealed interface IProgramOverride permits IUploadingOverride, ITransformOverride {

	void useProgram		();
	void setupProgram	();
}
