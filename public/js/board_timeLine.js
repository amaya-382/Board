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

    var token = $("#token").val();
    var deleteEvent = function () {
        if (!window.confirm("この投稿を削除します. よろしいですか？"))
            return;
        $.post(
            "/board/delete",
            { "id": $(this).parent().attr("id"), "token": token},
            function (res) {
                $("#" + res).remove();
            });
    };
    $(".delete").click(deleteEvent);

    $("#button_post").click(function () {
        console.log("clicked");
        $.post(
            "/board/post",
            {"content": $textarea.val(), "token": token},
            function (res) {
                var $post = $(res);
                $post.children(".delete").click(deleteEvent);
                $("#time_line").append($post);
                $textarea.val("");
                restore();
                scrollToBottom();
            });
        return false;
    });

    scrollToBottom();
});
