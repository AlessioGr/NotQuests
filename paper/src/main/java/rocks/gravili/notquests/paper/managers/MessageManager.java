package rocks.gravili.notquests.paper.managers;

import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.transformation.TransformationRegistry;
import net.kyori.adventure.text.minimessage.transformation.TransformationType;
import rocks.gravili.notquests.paper.NotQuests;


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
        TransformationType<?> unimportant = TransformationType.transformationType(
                TransformationType.acceptingNames("unimportant"),
                SimpleGradientTransformation::unimportant
        );

        TransformationType<?> warn = TransformationType.transformationType(
                TransformationType.acceptingNames("warn"),
                SimpleGradientTransformation::warn
        );
        TransformationType<?> veryUnimportant = TransformationType.transformationType(
                TransformationType.acceptingNames("veryUnimportant"),
                SimpleGradientTransformation::veryUnimportant
        );

        TransformationType<?> negative = TransformationType.transformationType(
                TransformationType.acceptingNames("negative"),
                SimpleGradientTransformation::negative
        );
        TransformationType<?> positive = TransformationType.transformationType(
                TransformationType.acceptingNames("positive"),
                SimpleGradientTransformation::positive
        );


        TransformationRegistry transformationRegistry = TransformationRegistry.standard().toBuilder()
                .add(mainGradient)
                .add(highlight)
                .add(highlight2)
                .add(error)
                .add(success)
                .add(unimportant)
                .add(veryUnimportant)
                .add(warn)
                .add(negative)
                .add(positive)

                .build();


        miniMessage = MiniMessage.builder().transformations(transformationRegistry).build();
    }
}
