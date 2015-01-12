$(function () {
    var $container = $("#container");
    var $textarea = $("textarea");
    var $post_form = $("#post_form");
    $textarea.focusin(function () {
        $container.css("height", "calc(100% - 200px)");
        $post_form.css("height", "150px");
    });
    $textarea.focusout(function () {
        $container.css("height", "calc(100% - 90px)");
        $post_form.css("height", "40px");
    });

    $(".delete").click(function () {
        $.post(
            "/board/delete",
            { "id": $(this).parent().attr("id"), "token": $("#token").val()},
            function (res) {
                $("#" + res).remove();
            });
    });

    setTimeout(function () {
        $container.scrollTop($container[0].scrollHeight);
    }, 100);
});
