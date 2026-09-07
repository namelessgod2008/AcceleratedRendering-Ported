package com.namelessgod2008.features.text;

import com.namelessgod2008.features.text.cache.ComponentMesh;
import net.minecraft.client.gui.Font.DisplayMode;

public interface IAcceleratedStringRenderOutput {

	void			setPosition		(float			positionX,	float positionY);
	void			setAccelerated	(boolean		accelerated);
	void			setOutline		(boolean		outline);
	void			setColor		(int			color);
	void			setMode			(DisplayMode	mode);
	void			flush			();
	void			beginMesh		();
	ComponentMesh	bake			();
}
