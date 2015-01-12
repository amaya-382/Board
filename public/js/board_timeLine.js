$(function () {
    var $container = $("#container");
    var $textarea = $("textarea");
    var $post_form = $("#post_form");
    $textarea.focusin(function () {
        $container.css("height", "calc(100% - 200px)");
        $post_form.css("height", "150px");
    });
    $("#header,#container").click(function () {
        $container.css("height", "calc(100% - 90px)");
        $post_form.css("height", "40px");
    });

    function scrollToBottom() {
        setTimeout(function () {
            $container.scrollTop($container[0].scrollHeight);
        }, 100);
    }

    var token = $("#token").val();
    $(".delete").click(function () {
        $.post(
            "/board/delete",
            { "id": $(this).parent().attr("id"), "token": token},
            function (res) {
                $("#" + res).remove();
            });
    });

    $("#button_post").click(function () {
        console.log("clicked");
        $.post(
            "/board/post",
            {"content": $textarea.val(), "token": token},
            function (res) {
                $("#time_line").append($(res));
                $textarea.val("");
                $container.css("height", "calc(100% - 90px)");
                $post_form.css("height", "40px");
                scrollToBottom();
            });
        return false;
    });

    scrollToBottom();
});
