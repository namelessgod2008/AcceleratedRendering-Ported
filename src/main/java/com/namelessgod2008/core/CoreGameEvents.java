package com.namelessgod2008.core;

//import com.namelessgod2008.AcceleratedRenderingModEntry;
//import com.namelessgod2008.core.utils.AvailabilityUtils;
//import net.minecraft.ChatFormatting;
//import net.minecraft.network.chat.Component;
//import net.neoforged.api.distmarker.Dist;
//import net.neoforged.bus.api.SubscribeEvent;
//import net.neoforged.fml.common.EventBusSubscriber;
//import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
//
//@EventBusSubscriber(
//		modid	= AcceleratedRenderingModEntry	.MOD_ID,
//		bus		= EventBusSubscriber.Bus		.GAME,
//		value	= Dist							.CLIENT
//)
//public class CoreEvents {
//
//	@SubscribeEvent
//	public static void onClientPlayerLoggedIn(ClientPlayerNetworkEvent.LoggingIn event) {
//		if (!AvailabilityUtils.isAvailable()) {
//			event.getPlayer().displayClientMessage(Component.translatable("acceleratedrendering.component.not_available").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD), false);
//		}
//	}
//}
