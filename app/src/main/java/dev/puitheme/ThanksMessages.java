// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 aiingjie

package dev.puitheme;

import java.util.Random;

/** Local dedications for every supporter. Choosing one never reads or sends personal information. */
public final class ThanksMessages {
    private static final String[] MESSAGES = {
            "Gracias por tu apoyo. Que este pequeño gesto aporte un poco de calidez a tu día a día.",
            "Gracias por encontrarte con este proyecto. Cada acto de bondad merece ser recordado.",
            "Gracias por acompañarnos. Que en los días que vienen encuentres cosas que te gusten y tranquilidad.",
            "Gracias por tu gesto. Que las pequeñas cosas buenas de la vida te sorprendan a menudo.",
            "Tu apoyo nos da más impulso para seguir puliendo este pequeño proyecto. ¡Gracias!",
            "Gracias por tu amabilidad. Que tus días ocupados rindan frutos y tus pausas te den alegría.",
            "Gracias por dejar una luz en este proyecto. Que cada uno de tus días también esté lleno de calidez.",
            "Gracias por acercar tu apoyo hasta aquí. Vamos a cuidar mucho este gesto.",
            "Gracias por acompañarnos. Que cada vez que enciendas la pantalla encuentres algo agradable.",
            "Gracias por tu apoyo. Que las cosas que te gustan vayan tomando la forma que imaginas.",
            "Cada apoyo es valioso. Gracias por darnos más ánimo para seguir probando cosas.",
            "Gracias por acercarte a este pequeño proyecto. Que este encuentro te regale un poco de alegría.",
            "Gracias por tu gesto. Que incluso en los días comunes encuentres muchos momentos brillantes.",
            "Gracias por tu apoyo. Que las ideas interesantes y la tranquilidad te acompañen siempre.",
            "Gracias por caminar con nosotros. Que tu día a día tenga más momentos agradables y menos preocupaciones.",
            "Gracias por este apoyo tan cálido. Que la generosidad que compartes también vuelva a ti con cariño.",
            "Gracias por tu amabilidad. Cada detalle cuidado lleva un poco de la calidez de tu apoyo.",
            "Gracias por sumar un compañero a este recorrido. Que en el camino siempre haya paisajes que esperar con ilusión.",
            "Gracias por apoyar el proyecto. Que esta pequeña creación te devuelva algo de buen ánimo.",
            "Gracias por tu gesto. Que siempre descubras algo nuevo y bonito en tus rutinas conocidas.",
            "Gracias por acompañarnos. Que sigas avanzando sin perder el tiempo para disfrutar del paisaje.",
            "Gracias por tu apoyo. Tu generosidad es una de las razones por las que seguimos trabajando con dedicación.",
            "Gracias por estar aquí. Que podamos compartir cada pequeño avance contigo.",
            "Gracias por este gesto. Que aquello que esperas vaya acercándose poco a poco.",
            "Gracias por tu calidez. Que tengas una vida cotidiana tranquila y también pequeños sueños propios.",
            "Gracias por acompañarnos. Que las alegrías sencillas aparezcan seguido en tu vida.",
            "Gracias por tu apoyo. Que cada vez que enciendas la pantalla disfrutes de un instante agradable.",
            "Gracias por tu amabilidad. Seguiremos cuidando los detalles con esa calidez presente.",
            "Gracias por detenerte en este pequeño proyecto. Que tú también encuentres cosas buenas que merezcan atención.",
            "Gracias por tu gesto. Que en tu vida haya pasión por lo que amas y espacio para bajar el ritmo.",
            "Gracias por tu apoyo. Que hoy ocurra alguna pequeña cosa que te saque una sonrisa.",
            "Gracias por estar a nuestro lado. Que cada momento cotidiano tenga su propio brillo.",
            "Gracias por este gesto tan valioso. Que encuentres calidez en tus encuentros y satisfacción en lo que haces.",
            "Gracias por darle a este proyecto un lugar en tu corazón. Ojalá puedas sentir el cuidado que ponemos en él.",
            "Gracias por tu amabilidad. Mientras persigues lo que te gusta, recuerda también cuidarte.",
            "Gracias por tu apoyo. Que encuentres un ritmo que te dé calma y te haga sentir bien.",
            "Gracias por acompañarnos. Que la vida reciba con ternura cada una de tus pequeñas ilusiones.",
            "Gracias por tu gesto. Que tengas ojos para descubrir la belleza y tiempo para disfrutarla.",
            "Gracias por estar con nosotros. Mientras sigamos avanzando, recordaremos tu apoyo.",
            "Gracias por este encuentro tan cálido. Que tu día a día siempre guarde alguna alegría para atesorar.",
            "Gracias por tu apoyo. Que aquello que amas y las cosas que te hacen sentir querido estén cerca de ti.",
            "Gracias por tu generosidad. Que este gesto deje calidez en nuestras vidas cotidianas.",
            "Gracias por acompañar este proyecto. Que cada actualización traiga una pequeña sorpresa nueva.",
            "Gracias por tu gesto. Que, aun en medio de las obligaciones, tengas espacio para lo que amas.",
            "Gracias por tu apoyo. Que salgas con ilusión y regreses con tranquilidad.",
            "Gracias por tu amabilidad. Que las pequeñas cosas bonitas se conviertan en buenas noticias de tu día.",
            "Gracias por apoyarnos. Es muy bueno contar con este gesto mientras seguimos trabajando con dedicación.",
            "Gracias por acompañarnos. Que cada día te reserves un momento para hacer algo por ti.",
            "Gracias por tu gesto. Que tengas ratos tranquilos y también proyectos en los que valga la pena involucrarse.",
            "Gracias por encontrarte con nosotros. Que esta pequeña creación acompañe tus días de una manera agradable.",
            "Gracias por tu apoyo. Que cada uno de tus pequeños deseos tenga la oportunidad de ser tomado en serio.",
            "Gracias por esta calidez. Que, al mirar atrás, encuentres motivos para sentirte satisfecho con lo vivido.",
            "Gracias por tu amabilidad. Que un detalle pequeño vuelva especial un día cualquiera.",
            "Gracias por acompañarnos. Que todos podamos mejorar un poco más aquello que nos gusta hacer.",
            "Gracias por tu gesto. Que tu vida tenga el bullicio justo y también una calma cómoda.",
            "Gracias por tu apoyo. Cada gesto hace que seguir explorando tenga más sentido.",
            "Gracias por dejar aquí un poco de calidez. Que tú también recibas mucha amabilidad espontánea.",
            "Gracias por acompañarnos. Que, sea cual sea tu ritmo, avances en la dirección que te gusta.",
            "Gracias por este apoyo tan valioso. Que cada día encuentres algo que mejore tu ánimo.",
            "Gracias por tu gesto. Que las cosas bonitas que esperas con ilusión acaben apareciendo en tu vida cotidiana.",
            "Gracias por caminar junto a nosotros. Que este pequeño recorrido deje recuerdos agradables.",
            "Gracias por tu amabilidad. Que incluso en los momentos cotidianos sientas que te aprecian.",
            "Gracias por tu apoyo. Que tengas el ajetreo justo y descansos en los que nadie te interrumpa.",
            "Gracias por este encuentro. Que aquello que amas te acompañe durante muchos buenos momentos.",
            "Gracias por tu gesto. Que puedas elegir con libertad y disfrutar de cada día con tranquilidad.",
            "Gracias por aportar calidez al proyecto. Que la pasión que llevas dentro siga brillando.",
            "Gracias por acompañarnos. Que cada pequeña mejora sea un motivo para alegrarte.",
            "Gracias por tu apoyo. Que la vida cotidiana te siga regalando sorpresas nuevas.",
            "Gracias por esta amabilidad. Que tengas ganas de levantar la vista para mirar el paisaje y tiempo para disfrutarlo.",
            "Gracias por apoyar este proyecto. Que sus detalles hagan tu día a día un poco más sencillo.",
            "Gracias por tu gesto. Que tengas paciencia para lo que quieres hacer y calma cuando necesites descansar.",
            "Gracias por tu calidez. Que tu día comience con buen ánimo y termine con tranquilidad.",
            "Gracias por caminar con nosotros. Que cada cosa sencilla que te gusta quede bien guardada.",
            "Gracias por tu apoyo. Que descubras pequeñas fortunas en cada día común.",
            "Gracias por tu amabilidad. Ojalá el cuidado de este proyecto te transmita un poco de calidez.",
            "Gracias por tu gesto. Que tus días tengan ritmo, ilusión y espacio para vivir sin apuro.",
            "Gracias por llegar hasta el muro de agradecimientos. Ojalá estas palabras te regalen un instante de ternura.",
            "Gracias por acompañarnos. Que sigas reuniendo alegrías sin perder aquello que te apasiona.",
            "Gracias por tu apoyo. Que las pequeñas cosas buenas ocupen un lugar importante en tu vida.",
            "Gracias por caminar con nosotros. Que cada intento hecho con dedicación abra una nueva posibilidad.",
            "Gracias por tu gesto. Que después de un día ajetreado siempre tengas un rincón donde relajarte.",
            "Gracias por esta generosidad. Que la vida te responda con más ligereza y calidez.",
            "Gracias por tu apoyo. Que tengas tiempo para apreciar los detalles y ganas de disfrutar las cosas sencillas.",
            "Gracias por acompañar el proyecto. Que juntos podamos verlo mejorar poco a poco.",
            "Gracias por tu calidez. Que los colores y paisajes que te gustan iluminen tus días.",
            "Gracias por tu gesto. Que conserves la ilusión y disfrutes de todo lo bueno que ya tienes.",
            "Gracias por apoyar el proyecto. Que esta pequeña mejora te aporte comodidad y alegría.",
            "Gracias por acompañarnos. Que siempre encuentres a tu alrededor alguna fuerza amable que te impulse.",
            "Gracias por esta generosidad. Que conviertas los días comunes en la vida que te gusta.",
            "Gracias por tu apoyo. Que aquello que amas siga teniendo espacio para crecer.",
            "Gracias por tu gesto. Que cada vez que te detengas descubras algo encantador de la vida.",
            "Gracias por este encuentro. Que tengas un mañana ilusionante y un presente que puedas valorar.",
            "Gracias por caminar con nosotros. Que estos detalles cuidados se conviertan poco a poco en una compañía familiar.",
            "Gracias por tu amabilidad. Que disfrutes con libertad cuando haces lo que te gusta.",
            "Gracias por tu apoyo. Recibe estas palabras de agradecimiento y nuestro compromiso de seguir mejorando.",
            "Gracias por tu calidez. Que las pequeñas alegrías de la vida te encuentren a menudo.",
            "Gracias por acompañarnos. Que, al seguir tu propio camino, encuentres paisajes que iluminen tu ánimo.",
            "Gracias por tu gesto. Que cada instante vivido con dedicación tenga un eco cálido y especial.",
            "Gracias por este apoyo. Que el encuentro entre tú y este pequeño proyecto conserve siempre una calidez especial.",
            "Gracias por dejar aquí tu generosidad. Ojalá cada detalle que cuidamos pueda responder a este valioso apoyo."
    };

    public static int count() { return MESSAGES.length; }

    public static String message(int index) { return MESSAGES[index]; }

    /** Every message remains possible; a valid previous choice is excluded without a retry loop. */
    public static int choose(Random random, int previous) {
        if (previous < 0 || previous >= MESSAGES.length) return random.nextInt(MESSAGES.length);
        int picked = random.nextInt(MESSAGES.length - 1);
        return picked >= previous ? picked + 1 : picked;
    }

    private ThanksMessages() {}
}
