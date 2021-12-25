package rocks.gravili.notquests.paper.managers;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.parser.Token;
import net.kyori.adventure.text.minimessage.parser.TokenType;
import net.kyori.adventure.text.minimessage.parser.node.TagPart;
import net.kyori.adventure.text.minimessage.placeholder.PlaceholderResolver;
import net.kyori.adventure.text.minimessage.transformation.Transformation;
import net.kyori.adventure.text.minimessage.transformation.TransformationFactory;
import net.kyori.adventure.text.minimessage.transformation.TransformationRegistry;
import net.kyori.adventure.text.minimessage.transformation.TransformationType;
import net.kyori.adventure.text.minimessage.transformation.inbuild.ColorTransformation;
import net.kyori.adventure.text.minimessage.transformation.inbuild.GradientTransformation;
import org.bukkit.Bukkit;
import rocks.gravili.notquests.paper.NotQuests;

import java.util.ArrayList;
import java.util.List;

public class MessageManager {
    private final NotQuests main;
    private final MiniMessage miniMessage;


    public final MiniMessage getMiniMessage(){
        return miniMessage;
    }

    public MessageManager(final NotQuests main){
        this.main = main;


        TransformationType<?> mainGradient = TransformationType.transformationType(
                TransformationType.acceptingNames("main"),
                SimpleGradientTransformation::main
        );
        TransformationType<?> highlight = TransformationType.transformationType(
                TransformationType.acceptingNames("highlight"),
                SimpleGradientTransformation::highlight
        );
        TransformationType<?> highlight2 = TransformationType.transformationType(
                TransformationType.acceptingNames("highlight2"),
                SimpleGradientTransformation::highlight2
        );
        TransformationType<?> error = TransformationType.transformationType(
                TransformationType.acceptingNames("error"),
                SimpleGradientTransformation::error
        );
        TransformationType<?> success = TransformationType.transformationType(
                TransformationType.acceptingNames("success"),
                SimpleGradientTransformation::success
        );


        TransformationRegistry transformationRegistry = TransformationRegistry.standard().toBuilder()
                .add(mainGradient)
                .add(highlight)
                .add(highlight2)
                .add(error)
                .add(success)
                .build();





        miniMessage = MiniMessage.builder().transformations(transformationRegistry).build();
    }
}
