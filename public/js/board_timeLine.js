$(function () {
    var $container = $("#container");
    var $textarea = $("textarea");
    var $post_form = $("#post_form");
    $textarea.focusin(function () {
        $container.css("height", "calc(100% - 200px)");
        $post_form.css("height", "150px");
    });
    var restore = function () {
        $container.css("height", "calc(100% - 90px)");
        $post_form.css("height", "40px");
    };
    $("#header,#container").click(restore);

    function scrollToBottom() {
        setTimeout(function () {
            $container.scrollTop($container[0].scrollHeight);
        }, 100);
    }

    var token = $("#login_user_name").data("token");
    var deleteEvent = function () {
        if (!window.confirm("この投稿を削除します. よろしいですか？"))
            return;
        $.post(
            "/board/delete",
            { "id": $(this).parent().attr("id"), "token": token},
            function (res) {
                var target = $("#" + res);
                target.animate({"height": 0, "opacity": 0}, 1500, function () {
                    target.remove();
                });
            });
    };
    $(".delete").click(deleteEvent);

    $("#button_post").click(function () {
        $.post(
            "/board/post",
            {"content": $textarea.val(), "token": token},
            function (res) {
                var $post = $(res).css({"width": 0, "opacity": 0});
                $post.children(".delete").click(deleteEvent);
                $textarea.val("");
                restore();
                scrollToBottom();
                $("#time_line").append($post);
                $post.animate({"width": "100%", "opacity": 1}, 1500);
            });
        return false;
    });

    scrollToBottom();
});
