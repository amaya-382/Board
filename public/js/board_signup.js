$(function () {
    $("#reinput").keyup(function () {
        var spans = $("#label_reinput").children("span");
        var $password = $("#password");
        if ($(this).val() === $password.val()) {
            spans.first().hide();
            spans.last().show();
            $(this).addClass("underline_green");
            $password.addClass("underline_green");
            $password.removeClass("underline_red");
            $(this).removeClass("underline_red");
        } else {
            spans.first().show();
            spans.last().hide();
            $(this).addClass("underline_red");
            $password.addClass("underline_red");
            $password.removeClass("underline_green");
            $(this).removeClass("underline_green");
        }
    });
});
