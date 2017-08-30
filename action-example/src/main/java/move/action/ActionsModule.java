package move.action;

import dagger.Module;

/**
 *
 */
@Module(
    includes = {
        _Move_Module.class
    },
    subcomponents = {
        _Move_Component.class
    }
)
public class ActionsModule {

}
